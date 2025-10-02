package com.example.Bookstore.controller;

import lombok.Data;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ViewController {

    // Public/Home
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("bestSellers", java.util.List.of(
                simpleItem(1L, "오브젝트"), simpleItem(2L, "클린 코드")
        ));
        model.addAttribute("trendingKeywords", java.util.List.of("자바", "스프링", "JPA"));
        model.addAttribute("recentViewed", java.util.List.of(
                recentItem(3L, "토비의 스프링"), recentItem(4L, "이펙티브 자바")
        ));
        return "product/home";
    }

    // Product/User-facing
    @GetMapping("/products/{id}")
    public String productDetail(@PathVariable("id") Long id, Model model) {
        model.addAttribute("product", demoProduct(id));
        model.addAttribute("reviews", java.util.List.of(
                review("홍길동", "좋은 책입니다."),
                review("이몽룡", "추천합니다!")
        ));
        return "product/detail";
    }

    @GetMapping("/search")
    public String searchList() { return "product/search-list"; }

    @GetMapping("/categories")
    public String categoryList() { return "product/category-list"; }

    @GetMapping("/cart")
    public String cart(Model model) {
        model.addAttribute("items", java.util.List.of());
        return "user/cart";
    }

    @GetMapping("/checkout")
    public String checkout(Model model) {
        model.addAttribute("checkout", null);
        model.addAttribute("totalPrice", 0);
        return "user/checkout";
    }

    @GetMapping("/mypage/orders/{id}")
    public String myOrderDetail(@PathVariable("id") Long id) { return "user/my-order-detail"; }

    // Admin
    @GetMapping("/admin/products")
    public String adminProducts(Model model) {
        model.addAttribute("items", java.util.List.of());
        paginateDefaults(model);
        return "admin/product-list";
    }

    @GetMapping("/admin/products/new")
    public String adminProductNew(Model model) {
        model.addAttribute("productForm", new ProductForm());
        return "admin/product-form";
    }

    @GetMapping("/admin/products/{id}")
    public String adminProductDetail(@PathVariable("id") Long id, Model model) {
        model.addAttribute("product", demoProduct(id));
        return "admin/product-detail";
    }

    @GetMapping("/admin/products/{id}/edit")
    public String adminProductEdit(@PathVariable("id") Long id, Model model) {
        model.addAttribute("productForm", new ProductForm());
        return "admin/product-form";
    }

    @GetMapping("/admin/orders")
    public String adminOrders(Model model) {
        model.addAttribute("items", java.util.List.of());
        paginateDefaults(model);
        return "admin/order-list";
    }

    @GetMapping("/admin/orders/{id}")
    public String adminOrderDetail(@PathVariable("id") Long id, Model model) {
        model.addAttribute("order", orderSummary(id));
        model.addAttribute("shipment", shipment());
        model.addAttribute("payment", payment());
        model.addAttribute("orderItems", java.util.List.of());
        return "admin/order-detail";
    }

    @GetMapping("/admin/members")
    public String adminMembers(Model model) {
        model.addAttribute("items", java.util.List.of());
        paginateDefaults(model);
        return "admin/member-list";
    }

    @GetMapping("/admin/members/{id}")
    public String adminMemberDetail(@PathVariable("id") Long id, Model model) {
        model.addAttribute("member", member(id));
        return "admin/member-detail";
    }

    @GetMapping("/admin/inventory")
    public String adminInventory(Model model) {
        model.addAttribute("items", java.util.List.of());
        paginateDefaults(model);
        return "admin/inventory-list";
    }

    // Footer links fallback
    @GetMapping({"/about", "/terms", "/privacy", "/help"})
    public String simplePages() { return "home"; }

    // Simple DTOs to satisfy th:object/th:field bindings
    @Data
    public static class ProductForm {
        private Long isbn;
        private String name;
        private String publisher;
        private String author;
        private Integer price;
        private String size;
        private Double rating;
        private Integer saleIndex;
        private String saleStatus;
        private String description;
    }

    // ---- Demo helpers
    private static java.util.Map<String, Object> simpleItem(Long id, String name) {
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("id", id);
        m.put("name", name);
        return m;
    }
    private static java.util.Map<String, Object> recentItem(Long productId, String productName) {
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("productId", productId);
        m.put("productName", productName);
        return m;
    }
    private static java.util.Map<String, Object> demoProduct(Long id) {
        java.util.Map<String, Object> p = new java.util.HashMap<>();
        p.put("id", id);
        p.put("name", "데모 책");
        p.put("image", "https://via.placeholder.com/240x336");
        p.put("thumbnail", "https://via.placeholder.com/120x168");
        p.put("price", 15000);
        p.put("stock", 10);
        p.put("size", "148x210mm");
        p.put("rating", 4.5);
        p.put("saleIndex", 1234);
        p.put("saleStatus", "ON_SALE");
        p.put("publisher", "데모출판사");
        p.put("author", "홍길동");
        p.put("isbn", 9781234567890L);
        p.put("description", "<p>데모 설명</p>");
        p.put("createdAt", java.time.LocalDateTime.now());
        return p;
    }
    private static java.util.Map<String, Object> review(String author, String text) {
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("author", author);
        m.put("text", text);
        return m;
    }
    private static void paginateDefaults(Model model) {
        if (!model.containsAttribute("page")) model.addAttribute("page", 0);
        if (!model.containsAttribute("pageSize")) model.addAttribute("pageSize", 30);
        if (!model.containsAttribute("totalPages")) model.addAttribute("totalPages", 0);
    }
    private static java.util.Map<String, Object> orderSummary(Long id) {
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("id", id);
        m.put("memberId", 1L);
        m.put("createdAt", java.time.LocalDateTime.now());
        m.put("status", "PENDING");
        m.put("totalPrice", 0);
        return m;
    }
    private static java.util.Map<String, Object> shipment() {
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("address", "서울시 어딘가 123-45");
        return m;
    }
    private static java.util.Map<String, Object> payment() {
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("method", "KAKAOPAY");
        m.put("status", "PENDING");
        m.put("amount", 0);
        return m;
    }
    private static java.util.Map<String, Object> member(Long id) {
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("id", id);
        m.put("name", "홍길동");
        m.put("email", "user@example.com");
        m.put("phone", "010-1234-5678");
        m.put("address", "서울시 어딘가 123-45");
        m.put("joinedAt", java.time.LocalDateTime.now().minusDays(10));
        m.put("lastLoginAt", java.time.LocalDateTime.now());
        m.put("grade", "BASIC");
        m.put("status", "ACTIVE");
        return m;
    }
}
