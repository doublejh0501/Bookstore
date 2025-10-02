package com.example.Bookstore.domain.order;

public enum OrderStatus {
    PENDING("결제 대기"),
    PAID("결제 완료"),
    SHIPPED("배송 중"),
    COMPLETED("배송 완료"),
    CANCELLED("취소");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
