# 🧠 Lab Manual: Business Logic Vulnerabilities

### 0. Preface
WAFs can block SQL injection, but not "buy one get ten free." Business logic vulnerabilities are the hardest for scanners to find because they're not technical errors—they're logical oversights.

*   **Business Logic Vulnerabilities**: Design or implementation flaws in application business processes that allow attackers to use systems in unintended ways.
*   **Core Concept**: The code has no bugs, but the business has vulnerabilities.

This chapter's goal: **Learn to identify exploitable logic flaws in business processes.**

---

### 1. Price Manipulation

**Scenario**: E-commerce website calculates prices on frontend or accepts them from client.

**Attack Example**:
```http
# Normal request
POST /order
{"productId": "123", "quantity": 1, "price": 999}

# Attack request — tamper price parameter
POST /order
{"productId": "123", "quantity": 1, "price": 0}
```

**More Subtle Ways**:
```
Modify total price: total_price: 999 → 1
Exploit discounts: discount: 10% → 100%
Tamper shipping: shipping: 20 → -500 (negative becomes cashback?)
```

**Why It Happens**:
Backend doesn't recalculate prices, trusting values from frontend.

---

### 2. Coupon Stacking

**Scenario**: Checkout allows coupon usage, frontend restricts to one coupon only.

**Key Thinking**:
```
1. Frontend restriction ≠ Backend restriction
2. Observe request data structure — does it use an array for coupons?
3. Does the backend deduplicate or limit the array?
4. Can the same coupon appear multiple times in the array?
```

**Attack Scenario**:
```http
# Normal frontend submission (single coupon)
POST /pay {"couponCodes": ["SAVE50"]}

# Attack: submit multiple/repeated coupons
POST /pay {"couponCodes": ["SAVE50", "SAVE50", "SAVE50", ...]}
```

**Core Question**: Does the backend accumulate discounts without total validation?

---

### 3. Flow Skip

**Normal Flow**:
```
Step 1: Select product → Step 2: Payment → Step 3: Confirm order
```

**Attack Method**:
```
Directly call Step 3's API, skipping payment
Prerequisite: Backend doesn't verify each step's preconditions
```

**Key Thinking**:
```
1. Does each step call a different API endpoint?
2. Does the backend independently verify prior step completion?
3. Frontend flow control (wizard, disabled buttons) is just a facade
```

---

### 4. Frontend Restrictions vs Backend Restrictions

**Eternal Lesson**: Any frontend restriction can be bypassed.

**Common "Fake Restrictions"**:
```javascript
// Frontend single-select → actual request is an array
body: JSON.stringify({ codes: [selectedCode] })

// Frontend disables button → just call the API directly
btn.disabled = true;

// Frontend validates amount → intercept and modify request
if (amount >= minPrice) { submit(); }
```

**Attack Methods**:
```
1. Use DevTools to observe request structure
2. Use Burp Suite / curl to craft requests directly
3. Modify request parameters to bypass frontend limits
```

---

### 5. General Attack Methodology

**Observe**:
```
- Which parameters in the request are controllable?
- What's the data structure? (object / array / nested)
- What's the difference between normal and abnormal responses?
```

**Test**:
```
- Modify amounts (0, negative, extremely large)
- Repeat submissions (call same operation multiple times)
- Skip steps (directly call subsequent endpoints)
- Array injection (single element → multiple elements)
```

**Verify**:
```
- Does the server truly validate each parameter?
- Are business rules enforced server-side?
- Are frontend restrictions only at the UI layer?
```

---

### 6. Defense Principles

1. **Server validates everything** — All prices, discounts, states calculated and checked on backend
2. **Flow integrity** — Each endpoint independently verifies preconditions are met
3. **Idempotency design** — Prevent accumulation effects from repeated submissions
4. **Input deduplication** — Array parameters must check for duplicate elements
5. **Total validation** — Even if stacking is allowed, limit total discount to not exceed original price

---

**[Ready? Go challenge those "rules"]**
