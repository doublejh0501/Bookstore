# Bookstore ERD  
  
```mermaid  
erDiagram
  USERS {
    BIGINT id PK
    VARCHAR username
    VARCHAR email
    VARCHAR password
    VARCHAR first_name
    VARCHAR last_name
    VARCHAR phone
    VARCHAR address
    VARCHAR status
    VARCHAR grade
    DATETIME created_at
    DATETIME last_login_at
  }

  USER_ROLES {
    BIGINT user_id FK
    VARCHAR role
  }

  CATEGORIES {
    BIGINT id PK
    VARCHAR name
    BIGINT parent_id FK
  }

  BOOKS {
    BIGINT id PK
    VARCHAR title
    TEXT description
    DECIMAL price
    VARCHAR isbn
    VARCHAR publisher
    VARCHAR image_url
    VARCHAR preview_pdf_url
    VARCHAR size
    DECIMAL rating
    INT sales_index
    VARCHAR sale_status
    DATE published_date
    BIGINT category_id FK
    DATETIME created_at
  }

  AUTHORS {
    BIGINT id PK
    VARCHAR name
    TEXT bio
  }

  BOOK_AUTHORS {
    BIGINT id PK
    BIGINT book_id FK
    BIGINT author_id FK
  }

  INVENTORIES {
    BIGINT id PK
    BIGINT book_id FK
    INT quantity
    DATETIME updated_at
  }

  CARTS {
    BIGINT id PK
    BIGINT user_id FK
    VARCHAR status
    DATETIME created_at
  }

  CART_ITEMS {
    BIGINT id PK
    BIGINT cart_id FK
    BIGINT book_id FK
    INT quantity
    DECIMAL unit_price
  }

  ORDERS {
    BIGINT id PK
    BIGINT user_id FK
    VARCHAR status
    DECIMAL total_amount
    DATETIME created_at
  }

  ORDER_ITEMS {
    BIGINT id PK
    BIGINT order_id FK
    BIGINT book_id FK
    INT quantity
    DECIMAL unit_price
  }

  PAYMENTS {
    BIGINT id PK
    BIGINT order_id FK
    VARCHAR status
    VARCHAR method
    DECIMAL amount
    VARCHAR provider_transaction_id
    DATETIME created_at
  }

  REVIEWS {
    BIGINT id PK
    BIGINT user_id FK
    BIGINT book_id FK
    INT rating
    TEXT content
    DATETIME created_at
  }

  RECENTLY_VIEWED {
    BIGINT id PK
    BIGINT user_id FK
    BIGINT book_id FK
    DATETIME viewed_at
  }

  SEARCH_KEYWORD_STATS {
    BIGINT id PK
    VARCHAR keyword
    BIGINT count
    DATETIME last_searched_at
  }

  %% Relationships
  USERS ||--o{ CARTS : "user_id"
  USERS ||--o{ ORDERS : "user_id"
  USERS ||--o{ USER_ROLES : "user_id"

  CATEGORIES ||--o{ BOOKS : "category_id"
  CATEGORIES ||--o{ CATEGORIES : "parent_id"

  BOOKS ||--o| INVENTORIES : "book_id"
  BOOKS ||--o{ REVIEWS : "book_id"
  USERS ||--o{ REVIEWS : "user_id"

  BOOKS ||--o{ CART_ITEMS : "book_id"
  CARTS ||--o{ CART_ITEMS : "cart_id"

  BOOKS ||--o{ ORDER_ITEMS : "book_id"
  ORDERS ||--o{ ORDER_ITEMS : "order_id"

  ORDERS ||--|| PAYMENTS : "order_id"
  USERS ||--o{ RECENTLY_VIEWED : "user_id"
  BOOKS ||--o{ RECENTLY_VIEWED : "book_id"

  BOOKS ||--o{ BOOK_AUTHORS : "book_id"
  AUTHORS ||--o{ BOOK_AUTHORS : "author_id"

  %% Note boxes (attribute names must be single words)
  NOTES {
    TEXT Unique_Constraints
    TEXT Single_Column
    TEXT USERS_username_UNIQUE
    TEXT USERS_email_UNIQUE
    TEXT CATEGORIES_name_UNIQUE_optional
    TEXT BOOKS_isbn_UNIQUE
    TEXT INVENTORIES_book_id_UNIQUE_BOOKS_zero_or_one_inventory
    TEXT PAYMENTS_order_id_UNIQUE_ORDERS_one_to_one_payment
    TEXT Composite_Constraints
    TEXT BOOK_AUTHORS_book_id_author_id
    TEXT CART_ITEMS_cart_id_book_id
    TEXT REVIEWS_user_id_book_id
    TEXT RECENTLY_VIEWED_user_id_book_id
  }

  LEGEND {
    TEXT Cardinalities
    TEXT barbar_exactly_one
    TEXT circle_zero_or_one
    TEXT brace_many
    TEXT Examples
    TEXT one_to_zero_or_one
    TEXT one_to_many
    TEXT one_to_one
  }

```  
  
## Notes  
  
- Table and column names reflect the current JPA mappings and unique constraints in the codebase.  
- Enums (roles, cart/order/payment statuses, payment method) are stored as strings.  
- Monetary fields use DECIMAL with appropriate scale per entity.
- 옵시디언에 그대로 복붙 ㄱㄱ