package com.evotesys.model;

import java.util.Date;

/** Maps to the ELECTION table. */
public class Election {

    private int idElection;
    private String title;
    private Date startDate;
    private Date endDate;
    private String status;
    private int contractId;

    public Election() {
    }

    public Election(int idElection, String title, Date startDate, Date endDate, String status, int contractId) {
        this.idElection = idElection;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.contractId = contractId;
    }

    public int getIdElection() {
        return idElection;
    }

    public void setIdElection(int idElection) {
        this.idElection = idElection;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getContractId() {
        return contractId;
    }

    public void setContractId(int contractId) {
        this.contractId = contractId;
    }

    // Drives how the election shows up in the JComboBox.
    @Override
    public String toString() {
        return idElection + " - " + title;
    }
}
