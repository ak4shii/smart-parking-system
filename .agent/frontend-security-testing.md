# Frontend Security Testing Plan - Smart Parking System

## Mục tiêu
Đảm bảo hệ thống frontend an toàn, bảo mật thông tin người dùng, và ngăn chặn truy cập trái phép vào các tài nguyên MQTT.

---

## 1. Test Authentication (Đăng ký/Đăng nhập)

### 1.1 Test Đăng ký (Registration)

#### Test Cases - Positive
- [ ] **TC-REG-001**: Đăng ký với thông tin hợp lệ
  - Input: Email, password đúng format
  - Expected: Tạo tài khoản thành công, redirect về trang login
  - Security Check: Password được hash trước khi gửi lên server

- [ ] **TC-REG-002**: Xác thực email format
  - Input: Các email format khác nhau (valid/invalid)
  - Expected: Chỉ chấp nhận email đúng format
  - Security Check: Client-side validation + server-side validation

#### Test Cases - Negative
- [ ] **TC-REG-003**: SQL Injection trong form đăng ký
  - Input: `admin'--`, `' OR '1'='1`, `'; DROP TABLE users--`
  - Expected: Request bị reject, không ảnh hưởng DB
  - Security Check: Input sanitization

- [ ] **TC-REG-004**: XSS Attack trong form
  - Input: `<script>alert('XSS')</script>` trong username/email
  - Expected: Script không được execute
  - Security Check: Input encoding/escaping

- [ ] **TC-REG-005**: Password strength testing
  - Input: Password yếu (123, abc, 111111)
  - Expected: Hiển thị warning, yêu cầu password mạnh hơn
  - Security Check: Password policy enforcement

- [ ] **TC-REG-006**: Rate limiting
  - Input: Gửi nhiều request đăng ký liên tiếp (>10 requests/minute)
  - Expected: Server block request, trả về 429 Too Many Requests
  - Security Check: Prevent brute force attacks

### 1.2 Test Đăng nhập (Login)

#### Test Cases - Positive
- [ ] **TC-LOGIN-001**: Login với credentials hợp lệ
  - Input: Email/password đúng
  - Expected: Nhận access token + refresh token, redirect về dashboard
  - Security Check: Token được lưu an toàn (httpOnly cookies hoặc secure storage)

- [ ] **TC-LOGIN-002**: Session management
  - Input: Login thành công
  - Expected: Session được tạo, token có expiry time
  - Security Check: Token expiration được validate

#### Test Cases - Negative
- [ ] **TC-LOGIN-003**: Brute force protection
  - Input: Nhập sai password 5 lần liên tiếp
  - Expected: Account bị lock tạm thời (5-15 phút)
  - Security Check: Account lockout mechanism

- [ ] **TC-LOGIN-004**: SQL Injection trong login form
  - Input: `admin' OR '1'='1'--` trong email field
  - Expected: Login failed, không bypass authentication
  - Security Check: Prepared statements/parameterized queries

- [ ] **TC-LOGIN-005**: Timing attack prevention
  - Input: Username tồn tại vs không tồn tại
  - Expected: Response time tương tự nhau
  - Security Check: Constant-time comparison

- [ ] **TC-LOGIN-006**: Session hijacking test
  - Input: Copy token sang browser khác
  - Expected: Validate device fingerprint, IP address
  - Security Check: Multi-factor session validation

---

## 2. Test API Security (Tạo Microcontroller)

### 2.1 Authorization Testing

- [ ] **TC-API-001**: Tạo microcontroller không có token
  - Input: Request không kèm Authorization header
  - Expected: 401 Unauthorized
  - Security Check: Authentication middleware

- [ ] **TC-API-002**: Tạo microcontroller với token không hợp lệ
  - Input: Token bị modify/expired
  - Expected: 401 Unauthorized
  - Security Check: Token validation

- [ ] **TC-API-003**: Tạo microcontroller với token hợp lệ
  - Input: Valid access token
  - Expected: 201 Created, trả về microcontroller info
  - Security Check: JWT verification

### 2.2 Access Control Testing

- [ ] **TC-API-004**: User A tạo microcontroller cho User B
  - Input: User A's token, User B's ID
  - Expected: 403 Forbidden
  - Security Check: User can only create devices for themselves

- [ ] **TC-API-005**: IDOR vulnerability test
  - Input: Modify microcontroller_id trong request
  - Expected: Chỉ access được device của mình
  - Security Check: Object-level authorization

### 2.3 Input Validation

- [ ] **TC-API-006**: Payload size limit
  - Input: Gửi payload > 1MB
  - Expected: 413 Payload Too Large
  - Security Check: Request body size limit

- [ ] **TC-API-007**: Invalid JSON payload
  - Input: Malformed JSON, unexpected fields
  - Expected: 400 Bad Request
  - Security Check: Schema validation

---

## 3. Test MQTT Credentials Security

### 3.1 Credentials Transmission

- [ ] **TC-MQTT-001**: MQTT credentials over HTTPS
  - Action: Monitor network traffic khi nhận credentials
  - Expected: Credentials chỉ được gửi qua HTTPS
  - Security Check: TLS/SSL encryption

- [ ] **TC-MQTT-002**: Credentials trong response
  - Action: Check API response
  - Expected: Password không được hiển thị plain text
  - Security Check: Sensitive data masking

### 3.2 Credentials Storage

- [ ] **TC-MQTT-003**: Client-side storage
  - Action: Check localStorage/sessionStorage
  - Expected: MQTT credentials không được lưu trong browser storage
  - Security Check: Secure credential management

- [ ] **TC-MQTT-004**: Memory exposure
  - Action: Check browser console, devtools
  - Expected: Credentials không bị log ra console
  - Security Check: No sensitive data logging

---

## 4. Test MQTT ACL (Access Control List)

### 4.1 Topic Authorization

- [ ] **TC-ACL-001**: Subscribe vào topic của chính mình
  - Topic: `user/{user_id}/devices/{device_id}/#`
  - Expected: Subscribe thành công
  - Security Check: ACL allow rule

- [ ] **TC-ACL-002**: Subscribe vào topic của user khác
  - Topic: `user/{other_user_id}/devices/#`
  - Expected: Connection refused hoặc subscribe failed
  - Security Check: ACL deny rule

- [ ] **TC-ACL-003**: Publish vào topic không có quyền
  - Topic: `user/{other_user_id}/devices/{device_id}/control`
  - Expected: Publish failed
  - Security Check: Write permission check

- [ ] **TC-ACL-004**: Wildcard topic abuse
  - Topic: `user/#` hoặc `#`
  - Expected: Subscribe failed
  - Security Check: Wildcard restriction

### 4.2 Pattern Testing

- [ ] **TC-ACL-005**: Topic pattern injection
  - Topic: `user/123/../../admin/#`
  - Expected: Pattern không được resolve
  - Security Check: Path traversal prevention

---

## 5. Test Session & Token Management

### 5.1 Token Lifecycle

- [ ] **TC-TOKEN-001**: Access token expiration
  - Action: Đợi token expire (thường 15-30 phút)
  - Expected: Auto refresh token hoặc redirect về login
  - Security Check: Token expiry enforcement

- [ ] **TC-TOKEN-002**: Refresh token rotation
  - Action: Sử dụng refresh token
  - Expected: Nhận access token mới + refresh token mới
  - Security Check: One-time use refresh token

- [ ] **TC-TOKEN-003**: Concurrent sessions
  - Action: Login trên 2 devices
  - Expected: Policy-based (allow/deny multiple sessions)
  - Security Check: Session management

### 5.2 Logout & Revocation

- [ ] **TC-TOKEN-004**: Logout functionality
  - Action: Click logout
  - Expected: Token bị revoke, redirect về login
  - Security Check: Token invalidation

- [ ] **TC-TOKEN-005**: Token reuse after logout
  - Action: Dùng token đã logout để call API
  - Expected: 401 Unauthorized
  - Security Check: Token blacklist

---

## 6. Test CORS & CSRF Protection

### 6.1 CORS Testing

- [ ] **TC-CORS-001**: Request từ origin không được phép
  - Origin: `http://malicious-site.com`
  - Expected: CORS error
  - Security Check: CORS whitelist

- [ ] **TC-CORS-002**: Request từ origin hợp lệ
  - Origin: `https://your-frontend-domain.com`
  - Expected: Request thành công
  - Security Check: CORS configuration

### 6.2 CSRF Testing

- [ ] **TC-CSRF-001**: State-changing request không có CSRF token
  - Action: POST/PUT/DELETE without CSRF token
  - Expected: 403 Forbidden
  - Security Check: CSRF middleware

- [ ] **TC-CSRF-002**: CSRF token validation
  - Action: Submit form với CSRF token
  - Expected: Request thành công
  - Security Check: Token generation & validation

---

## 7. Test Error Handling & Information Disclosure

### 7.1 Error Messages

- [ ] **TC-ERROR-001**: Login failed error
  - Input: Sai email hoặc password
  - Expected: Generic message "Invalid credentials" (không tiết lộ email có tồn tại hay không)
  - Security Check: Information leakage prevention

- [ ] **TC-ERROR-002**: Server error handling
  - Action: Gây lỗi server (500 Internal Server Error)
  - Expected: Generic error message, không hiển thị stack trace
  - Security Check: Error sanitization

### 7.2 Information Exposure

- [ ] **TC-INFO-001**: API response enumeration
  - Action: Check API responses
  - Expected: Không tiết lộ thông tin nhạy cảm (internal IDs, DB structure)
  - Security Check: Data minimization

- [ ] **TC-INFO-002**: Browser console logs
  - Action: Mở browser devtools
  - Expected: Không có log sensitive data
  - Security Check: Production build optimization

---

## 8. Test Client-Side Security

### 8.1 XSS Prevention

- [ ] **TC-XSS-001**: Stored XSS
  - Input: Lưu `<script>alert('XSS')</script>` vào DB
  - Expected: Script được encode khi hiển thị
  - Security Check: Output encoding

- [ ] **TC-XSS-002**: Reflected XSS
  - URL: `?search=<script>alert('XSS')</script>`
  - Expected: Script không execute
  - Security Check: URL parameter sanitization

- [ ] **TC-XSS-003**: DOM-based XSS
  - Action: Manipulate DOM với untrusted data
  - Expected: Data được sanitize
  - Security Check: DOMPurify or similar

### 8.2 Dependency Security

- [ ] **TC-DEP-001**: Vulnerable dependencies
  - Action: Run `npm audit`
  - Expected: Không có critical/high vulnerabilities
  - Security Check: Regular dependency updates

- [ ] **TC-DEP-002**: Third-party scripts
  - Action: Check external scripts (CDN, analytics)
  - Expected: Sử dụng SRI (Subresource Integrity)
  - Security Check: Script integrity verification

---

## 9. Test HTTPS & Transport Security

### 9.1 SSL/TLS Configuration

- [ ] **TC-TLS-001**: Force HTTPS
  - Action: Access site qua HTTP
  - Expected: Auto redirect to HTTPS
  - Security Check: HSTS header

- [ ] **TC-TLS-002**: Certificate validation
  - Action: Check SSL certificate
  - Expected: Valid certificate, không expired
  - Security Check: TLS 1.2+ only

### 9.2 Security Headers

- [ ] **TC-HEADER-001**: Check security headers
  - Headers to verify:
    - `Strict-Transport-Security`
    - `X-Content-Type-Options: nosniff`
    - `X-Frame-Options: DENY`
    - `Content-Security-Policy`
    - `X-XSS-Protection: 1; mode=block`
  - Expected: Tất cả headers được set đúng
  - Security Check: HTTP header security

---

## 10. Production-Specific Tests

### 10.1 Mosquitto Dynamic Security Plugin

> [!IMPORTANT]
> Production nên dùng **Mosquitto Dynamic Security Plugin** thay vì `docker restart`

- [ ] **TC-PROD-001**: Auto user sync to Mosquitto
  - Action: Tạo user mới
  - Expected: MQTT credentials available ngay lập tức (không cần restart)
  - Security Check: Real-time ACL updates

- [ ] **TC-PROD-002**: ACL reload verification
  - Action: Update ACL rules
  - Expected: Rules áp dụng ngay lập tức
  - Security Check: Zero-downtime ACL updates

### 10.2 Alternative: SIGHUP Signal

- [ ] **TC-PROD-003**: SIGHUP signal from backend
  - Action: Backend gửi SIGHUP signal sau khi update password file
  - Expected: Mosquitto reload config without restart
  - Security Check: Graceful configuration reload

---

## Testing Tools & Automation

### Recommended Tools

1. **OWASP ZAP** - Automated security testing
2. **Burp Suite** - Manual penetration testing
3. **Postman/Insomnia** - API testing
4. **MQTT.fx / MQTT Explorer** - MQTT testing
5. **Chrome DevTools** - Network/security analysis
6. **npm audit / Snyk** - Dependency scanning

### Automation Scripts

```bash
# Security testing script
#!/bin/bash

echo "=== Frontend Security Test Suite ==="

# 1. Dependency audit
echo "Running npm audit..."
npm audit --audit-level=moderate

# 2. Check for hardcoded secrets
echo "Checking for secrets..."
grep -r "password\|secret\|api_key" src/ --exclude-dir=node_modules

# 3. Build production bundle
echo "Building production..."
npm run build

# 4. Check build size (security implications)
echo "Analyzing bundle size..."
npm run analyze

# 5. Run linter with security rules
echo "Running ESLint security checks..."
npm run lint

echo "=== Tests completed ==="
```

---

## Security Testing Checklist Summary

### High Priority ⚠️
- [ ] Authentication bypass testing
- [ ] MQTT ACL verification
- [ ] SQL Injection prevention
- [ ] XSS protection
- [ ] Token security
- [ ] HTTPS enforcement

### Medium Priority
- [ ] CSRF protection
- [ ] Rate limiting
- [ ] Error handling
- [ ] CORS configuration
- [ ] Session management

### Low Priority
- [ ] Information disclosure
- [ ] Dependency updates
- [ ] Security headers
- [ ] Logging practices

---

## Test Reporting Template

```markdown
## Security Test Report - [Date]

### Executive Summary
- Total tests: X
- Passed: Y
- Failed: Z
- Critical issues: N

### Critical Findings
1. [Issue description]
   - Severity: Critical/High/Medium/Low
   - Affected component: [Component name]
   - Reproduction steps: [Steps]
   - Recommendation: [Fix]

### Test Coverage
- Authentication: X%
- Authorization: X%
- API Security: X%
- MQTT ACL: X%
- XSS/CSRF: X%

### Next Steps
- [Action items]
```

---

## References

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [OWASP Testing Guide](https://owasp.org/www-project-web-security-testing-guide/)
- [Mosquitto Security](https://mosquitto.org/documentation/authentication-methods/)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)
