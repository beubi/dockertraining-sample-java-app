package com.sibs.sample;

public class SampleApp {
    private final String message = "Hello World!";

    public SampleApp() {
    }

    public static void main(String[] args) {
        System.out.println(new SampleApp().getMessage());
    }

    private final String getMessage() {
        return message;
    }
}
