package com.example.csit228capstone.model;

import com.example.csit228capstone.model.vulnerability.VulnerabilityTag;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Resident {
    private UUID id;
    private UUID householdId;
    private String firstName;
    private String middleName;
    private String lastName;
    private String suffix;
    private LocalDate dateOfBirth;
    private String sex;
    private String civilStatus;
    private String contactNumber;
    private String photoUrl;
    private boolean isHouseholdHead;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<VulnerabilityTag> vulnerabilities = new ArrayList<>();


    public Resident() {}

    // --- Getters & Setters ---
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getHouseholdId() { return householdId; }
    public void setHouseholdId(UUID householdId) { this.householdId = householdId; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getSuffix() { return suffix; }
    public void setSuffix(String suffix) { this.suffix = suffix; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }
    public String getCivilStatus() { return civilStatus; }
    public void setCivilStatus(String civilStatus) { this.civilStatus = civilStatus; }
    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public boolean isHouseholdHead() { return isHouseholdHead; }
    public void setHouseholdHead(boolean householdHead) { isHouseholdHead = householdHead; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<VulnerabilityTag> getVulnerabilities() { return vulnerabilities; }
    public void setVulnerabilities(List<VulnerabilityTag> vulnerabilities) { this.vulnerabilities = vulnerabilities; }

    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        sb.append(firstName);
        if (middleName != null && !middleName.isEmpty()) sb.append(" ").append(middleName);
        sb.append(" ").append(lastName);
        if (suffix != null && !suffix.isEmpty()) sb.append(" ").append(suffix);
        return sb.toString();
    }

    public int getAge() {
        return java.time.Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
}
