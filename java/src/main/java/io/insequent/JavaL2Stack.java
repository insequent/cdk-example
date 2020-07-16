package io.insequent;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.iam.*;

import java.util.Arrays;

public class JavaL2Stack extends Stack {
    final static Integer port = 8888;

    public JavaL2Stack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public JavaL2Stack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);
        final String prefix = getStackName();

        CfnVPC vpc = CfnVPC.Builder.create(this, prefix + "Vpc")
                .cidrBlock("10.10.0.0/16")
                .build();
        CfnSubnet subnet = CfnSubnet.Builder.create(this, prefix + "PublicSubnet")
                .availabilityZone(getAvailabilityZones().get(0))
                .cidrBlock("10.10.0.0/17")
                .mapPublicIpOnLaunch(true)
                .vpcId(vpc.getRef())
                .build();
        CfnRouteTable routeTable = CfnRouteTable.Builder.create(this, prefix + "RouteTable")
                .vpcId(vpc.getRef())
                .build();
        CfnSubnetRouteTableAssociation routeTableAssociation = CfnSubnetRouteTableAssociation.Builder
                .create(this, prefix + "RouteTableAssociation")
                .routeTableId(routeTable.getRef())
                .subnetId(subnet.getRef())
                .build();
        CfnRoute route = CfnRoute.Builder.create(this, prefix + "Route")
                .destinationCidrBlock("0.0.0.0/0")
                .routeTableId(routeTable.getRef())
                .build();
        CfnEIP eip = CfnEIP.Builder.create(this, prefix + "Eip")
                .domain("vpc")
                .build();
        CfnSecurityGroup sg = CfnSecurityGroup.Builder.create(this, prefix + "SecurityGroup")
                .securityGroupEgress(Arrays.asList(
                        CfnSecurityGroup.EgressProperty.builder()
                                .cidrIp("0.0.0.0/0")
                                .ipProtocol("tcp")
                                .toPort(8888)
                                .build()
                ))
                .groupName(prefix + "SecurityGroup")
                .build();
        CfnRole role = CfnRole.Builder.create(this, prefix + "Role")
                .assumeRolePolicyDocument(PolicyStatement.Builder.create()
                        .actions(Arrays.asList("sts:AssumeRole"))
                        .effect(Effect.ALLOW)
                        .principals(Arrays.asList(
                                ServicePrincipal.Builder.create("ec2.amazonaws.com").build()
                        ))
                        .build()
                )
                .description("Allow HelloWorld web traffic")
                .path("/role/" + getStackName())
                .roleName(prefix + "Role")
                .build();
        CfnInstanceProfile instanceProfile = CfnInstanceProfile.Builder.create(this, prefix + "InstanceProfile")
                .instanceProfileName(prefix + "InstanceProfile")
                .path("/instance-profile/" + getStackName())
                .roles(Arrays.asList(role.getRoleName()))
                .build();
        CfnInstance instance = CfnInstance.Builder.create(this, prefix + "Instance")
                .imageId("ami-0e7f40bffc03ebc9d")
                .instanceType("t2.micro")
                .securityGroups(Arrays.asList(sg.getRef()))
                .subnetId(subnet.getRef())
                .userData("#!/bin/bash\n" +
                        "apt install python3 -y\n" +
                        "mkdir /tmp/helloworld\n" +
                        "cd /tmp/helloworld\n" +
                        "echo 'HELLO WORLD!!!' > TEST\n" +
                        "/usr/bin/python3 -m http.server " + port + "\n"
                )
                .build();
    }
}
