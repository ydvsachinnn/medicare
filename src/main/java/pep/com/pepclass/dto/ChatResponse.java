package pep.com.pepclass.dto;

public class ChatResponse {

    private String response;
    private Integer remainingQuota;
    private Integer maxQuota = 50;

    public ChatResponse() {
    }

    public ChatResponse(String response) {
        this.response = response;
    }

    public ChatResponse(String response, Integer remainingQuota, Integer maxQuota) {
        this.response = response;
        this.remainingQuota = remainingQuota;
        this.maxQuota = maxQuota;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public Integer getRemainingQuota() {
        return remainingQuota;
    }

    public void setRemainingQuota(Integer remainingQuota) {
        this.remainingQuota = remainingQuota;
    }

    public Integer getMaxQuota() {
        return maxQuota;
    }

    public void setMaxQuota(Integer maxQuota) {
        this.maxQuota = maxQuota;
    }
}
