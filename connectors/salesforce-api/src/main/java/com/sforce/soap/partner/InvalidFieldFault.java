
package com.sforce.soap.partner;

import javax.xml.ws.WebFault;


/**
 * This class was generated by Apache CXF 2.2.6
 * Thu Feb 11 15:37:56 EST 2010
 * Generated source version: 2.2.6
 * 
 */

@WebFault(name = "InvalidFieldFault", targetNamespace = "urn:fault.partner.soap.sforce.com")
public class InvalidFieldFault extends Exception {
    public static final long serialVersionUID = 20100211153756L;
    
    private com.sforce.soap.partner.fault.InvalidFieldFault invalidFieldFault;

    public InvalidFieldFault() {
        super();
    }
    
    public InvalidFieldFault(String message) {
        super(message);
    }
    
    public InvalidFieldFault(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidFieldFault(String message, com.sforce.soap.partner.fault.InvalidFieldFault invalidFieldFault) {
        super(message);
        this.invalidFieldFault = invalidFieldFault;
    }

    public InvalidFieldFault(String message, com.sforce.soap.partner.fault.InvalidFieldFault invalidFieldFault, Throwable cause) {
        super(message, cause);
        this.invalidFieldFault = invalidFieldFault;
    }

    public com.sforce.soap.partner.fault.InvalidFieldFault getFaultInfo() {
        return this.invalidFieldFault;
    }
}
