
package com.sforce.soap.partner;

import javax.xml.ws.WebFault;


/**
 * This class was generated by Apache CXF 2.2.6
 * Thu Feb 11 15:37:56 EST 2010
 * Generated source version: 2.2.6
 * 
 */

@WebFault(name = "MalformedQueryFault", targetNamespace = "urn:fault.partner.soap.sforce.com")
public class MalformedQueryFault extends Exception {
    public static final long serialVersionUID = 20100211153756L;
    
    private com.sforce.soap.partner.fault.MalformedQueryFault malformedQueryFault;

    public MalformedQueryFault() {
        super();
    }
    
    public MalformedQueryFault(String message) {
        super(message);
    }
    
    public MalformedQueryFault(String message, Throwable cause) {
        super(message, cause);
    }

    public MalformedQueryFault(String message, com.sforce.soap.partner.fault.MalformedQueryFault malformedQueryFault) {
        super(message);
        this.malformedQueryFault = malformedQueryFault;
    }

    public MalformedQueryFault(String message, com.sforce.soap.partner.fault.MalformedQueryFault malformedQueryFault, Throwable cause) {
        super(message, cause);
        this.malformedQueryFault = malformedQueryFault;
    }

    public com.sforce.soap.partner.fault.MalformedQueryFault getFaultInfo() {
        return this.malformedQueryFault;
    }
}
