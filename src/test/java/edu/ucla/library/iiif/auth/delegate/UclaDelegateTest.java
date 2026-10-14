package edu.ucla.library.iiif.auth.delegate;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class UclaDelegateTest {
    

    @Test
    public void testUclaIP() {
    	UclaDelegate delegate = new UclaDelegate();
        assertTrue( delegate.isUclaIp("66.228.56.140"));
    }

    @Test
    public void testNotUclaIP() {
    	UclaDelegate delegate = new UclaDelegate();
        assertFalse( delegate.isUclaIp("127.0.0.1"));
    }
}
