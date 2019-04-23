package com.sibs.sample;

import org.junit.Test;

public class SampleAppTest {

    @Test
    public void testSampleApp() {
        try {
            new SampleApp();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
