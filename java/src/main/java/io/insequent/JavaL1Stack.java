package io.insequent;

import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.iam.*;

import java.util.Arrays;

public class JavaL1Stack extends Stack {
    final static Integer port = 8888;

    public JavaL1Stack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public JavaL1Stack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);
        final String prefix = getStackName();

        // Networking
        CfnVPC vpc = CfnVPC.Builder.create(this, prefix + "Vpc")
                .cidrBlock("10.10.0.0/16")
                .enableDnsHostnames(true)
                .enableDnsSupport(true)
                .instanceTenancy("default")
                .build();
        Tag.add(vpc, "Name", prefix + "Vpc");
        CfnInternetGateway igw = CfnInternetGateway.Builder.create(this, prefix + "InternetGateway")
                .build();
        CfnVPCGatewayAttachment gwa = CfnVPCGatewayAttachment.Builder.create(this, prefix + "InternetGatewayAttachment")
                .internetGatewayId(igw.getRef())
                .vpcId(vpc.getRef())
                .build();
        CfnSubnet subnet = CfnSubnet.Builder.create(this, prefix + "PublicSubnet")
                .availabilityZone(getAvailabilityZones().get(0))
                .cidrBlock("10.10.0.0/17")
                .mapPublicIpOnLaunch(true)
                .vpcId(vpc.getRef())
                .build();
        Tag.add(subnet, "Name", prefix + "-public-subnet");
        CfnRouteTable routeTable = CfnRouteTable.Builder.create(this, prefix + "RouteTable")
                .vpcId(vpc.getRef())
                .build();
        CfnSubnetRouteTableAssociation routeTableAssociation = CfnSubnetRouteTableAssociation.Builder.create(this, prefix + "RouteTableAssociation")
                .routeTableId(routeTable.getRef())
                .subnetId(subnet.getRef())
                .build();
        CfnEIP natEip = CfnEIP.Builder.create(this, prefix + "Eip")
                .domain("vpc")
                .build();
        CfnNatGateway natGw = CfnNatGateway.Builder.create(this, prefix + "NatGateway")
                .allocationId(natEip.getAttrAllocationId())
                .subnetId(subnet.getRef())
                .build();
        CfnRoute route = CfnRoute.Builder.create(this, prefix + "Route")
                .destinationCidrBlock("0.0.0.0/0")
                .gatewayId(igw.getRef())
                .routeTableId(routeTable.getRef())
                .build();
        CfnSecurityGroup sg = CfnSecurityGroup.Builder.create(this, prefix + "SecurityGroup")
                .securityGroupIngress(Arrays.asList(
                        CfnSecurityGroup.IngressProperty.builder()
                                .cidrIp("0.0.0.0/0")
                                .description("Allow HelloWorld web traffic")
                                .fromPort(port)
                                .ipProtocol("tcp")
                                .toPort(port)
                                .build()
                ))
                .securityGroupEgress(Arrays.asList(
                        CfnSecurityGroup.EgressProperty.builder()
                                .cidrIp("0.0.0.0/0")
                                .description("Allow all outbound by default")
                                .ipProtocol("-1")
                                .build()
                ))
                .groupDescription("Security group for " + getStackName())
                .vpcId(vpc.getRef())
                .build();
        Tag.add(sg, "Name", prefix + "SecurityGroup");

        // Access
        CfnRole role = CfnRole.Builder.create(this, prefix + "InstanceRole")
                .assumeRolePolicyDocument(PolicyDocument.Builder.create()
                        .statements(Arrays.asList(PolicyStatement.Builder.create()
                                .actions(Arrays.asList("sts:AssumeRole"))
                                .effect(Effect.ALLOW)
                                .principals(Arrays.asList(
                                        ServicePrincipal.Builder.create("ec2.amazonaws.com").build()
                                ))
                                .build()
                        ))
                        .build()
                )
                .path("/roles/" + getStackName() + "/")
                .roleName(prefix + "Role")
                .build();

        // Instance
        CfnInstanceProfile instanceProfile = CfnInstanceProfile.Builder.create(this, prefix + "InstanceProfile")
                .instanceProfileName(prefix + "InstanceProfile")
                .path("/profiles/" + getStackName() + "/")
                .roles(Arrays.asList(role.getRef()))
                .build();
        CfnInstance instance = CfnInstance.Builder.create(this, prefix + "Instance")
                .imageId("ami-0e7f40bffc03ebc9d")
                .iamInstanceProfile(instanceProfile.getRef())
                .instanceType("t2.micro")
                .securityGroupIds(Arrays.asList(sg.getRef()))
                .subnetId(subnet.getRef())
                .userData(Fn.base64(
                        "#!/bin/bash\n" +
                        "apt install python3 -y\n" +
                        "mkdir /tmp/helloworld\n" +
                        "cd /tmp/helloworld\n" +
                        "echo 'HELLO WORLD from " + getStackName() + "!!!' > TEST\n" +
                        "/usr/bin/python3 -m http.server " + port + "\n"
                ))
                .build();
        Tag.add(instance, "Name", prefix + "Instance");
    }
}
