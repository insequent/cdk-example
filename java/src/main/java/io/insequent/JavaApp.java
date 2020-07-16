package io.insequent;

import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;

public class JavaApp {
    public static void main(final String[] args) {
        App app = new App();
        StackProps props = createProps("ap-southeast-2");

        new JavaL1Stack(app, "JavaL1Stack", props);
        //new JavaL2Stack(app, "JavaL2Stack", props);

        app.synth();
    }

    private static StackProps createProps(final String region) {
        return StackProps.builder()
                .env(Environment.builder()
                        .region(region)
                        .build()
                )
                .build();
    }
}
