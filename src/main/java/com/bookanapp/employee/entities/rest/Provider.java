package com.bookanapp.employee.entities.rest;


import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;


@Data

public class Provider implements Cloneable, Serializable {

    private long entityId;
    private long id;
    private String username;
    private String name;
    private String mainImage;
    private String description;
    private int numberOfViews;
    private LocalDateTime registerDate;
    private String displayServiceTypes;
    private boolean visible;
    private LocalDateTime gracePeriod;
    private LocalDateTime paidUntil;
    private String locale;
    private String invitationLink;
    private String vat;
    private String companyName;
    private Integer smsCount;

    private String lastSessionId;
    private boolean addressVisible;
    private boolean allowAnonimousBooking;
    private float smsCredits;
    private boolean individual;
    private String referralCode;
    private String usedReferralCode;
    private String emailVerificationToken;

    public Provider(long id) {
        this.id = id;
    }

    @Override
    public int hashCode()
    {
        return this.username.hashCode();
    }


    @Override
    public boolean equals(Object other)
    {
        return other instanceof Provider &&
                ((Provider)other).id == this.id;
    }

    @Override
    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    protected Provider clone()
    {
        try {
            return (Provider) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e); // not possible
        }
    }

    @Override
    public String toString()
    {
        return this.username;
    }


    public enum Type{
        BASIC,
        PLUS,
        PRO,
        BUSINESS,
        ENTERPRISE
    }

}
