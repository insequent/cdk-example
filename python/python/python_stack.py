#!/usr/bin/env python


from aws_cdk import (
    aws_ec2 as ec2,
    core,
)


class PythonStack(core.Stack):

    def __init__(self, scope: core.Construct, id: str, **kwargs) -> None:
        super().__init__(scope, id, **kwargs)
        port = 8888
        prefix = self.stack_name

        vpc = ec2.Vpc(self, prefix + "Vpc",
                      cidr="10.10.0.0/16",
                      nat_gateways=1,
                      subnet_configuration=[
                          ec2.SubnetConfiguration(name="PublicSubnet",
                                                  subnet_type=ec2.SubnetType.PUBLIC)
                      ])

        sg = ec2.SecurityGroup(self, prefix + "SecurityGroup",
                               allow_all_outbound=True,
                               vpc=vpc)
        sg.add_ingress_rule(ec2.Peer.any_ipv4(), ec2.Port.tcp(port), "Allow HelloWorld web traffic")

        ec2.Instance(self, prefix + "Instance",
                     instance_type=ec2.InstanceType("t2.micro"),
                     machine_image=ec2.MachineImage.generic_linux({"ap-southeast-2": "ami-0e7f40bffc03ebc9d"}),
                     security_group=sg,
                     user_data=ec2.UserData.custom(
                         "#!/bin/bash\n"
                         "apt install python3 -y\n"
                         "mkdir /tmp/helloworld\n"
                         "cd /tmp/helloworld\n"
                         f"echo 'HELLO WORLD from {self.stack_name}!!!' > TEST\n"
                         f"/usr/bin/python3 -m http.server {port}\n"
                     ),
                     vpc_subnets=ec2.SubnetSelection(subnet_type=ec2.SubnetType.PUBLIC),
                     vpc=vpc)
