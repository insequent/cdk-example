import * as cdk from '@aws-cdk/core';
 
import ec2 = require('@aws-cdk/aws-ec2');

export class TypescriptStack extends cdk.Stack {
  constructor(scope: cdk.Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const vpc = new ec2.Vpc(this, 'MyVpc', { maxAzs: 2 });
  }
}
