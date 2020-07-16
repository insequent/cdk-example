package io.insequent;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.*;

import java.util.Arrays;
import java.util.HashMap;

public class JavaL1Stack extends Stack {
    final static Integer port = 8888;

    public JavaL1Stack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public JavaL1Stack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);
        final String prefix = getStackName();

        Vpc vpc = Vpc.Builder.create(this, prefix + "Vpc")
                .cidr("10.10.0.0/16")
                .natGateways(1)
                .subnetConfiguration(Arrays.asList(
                        SubnetConfiguration.builder()
                                .name("PublicSubnet")
                                .subnetType(SubnetType.PUBLIC)
                                .build()
                ))
                .build();

        SecurityGroup sg = SecurityGroup.Builder.create(this, prefix + "SecurityGroup")
                .allowAllOutbound(true)
                .vpc(vpc)
                .build();
        sg.addIngressRule(Peer.anyIpv4(), Port.tcp(port), "Allow HelloWorld web traffic");

        Instance instance = Instance.Builder.create(this, prefix + "Service")
                .instanceType(new InstanceType("t2.micro"))
                .machineImage(MachineImage.genericLinux(new HashMap<String, String>() {{
                    put("ap-southeast-2", "ami-0e7f40bffc03ebc9d");
                }}))
                .securityGroup(sg)
                .userData(UserData.custom("#!/bin/bash\n" +
                    "apt install python3 -y\n" +
                    "mkdir /tmp/helloworld\n" +
                    "cd /tmp/helloworld\n" +
                    "echo 'HELLO WORLD from " + getStackName() + "!!!' > TEST\n" +
                    "/usr/bin/python3 -m http.server " + port + "\n"
                ))
                .vpcSubnets(SubnetSelection.builder()
                        .subnetType(SubnetType.PUBLIC)
                        .build()
                )
                .vpc(vpc)
                .build();
    }
}
