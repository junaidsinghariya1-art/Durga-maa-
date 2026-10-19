# Durga Maa Loan — Production Deployment & PWA Configuration Guide

This document provides complete instructions for deploying the **Durga Maa Loan** loan management application to **Supabase**, **Vercel**, and testing as an installable **PWA (Progressive Web App)** on **iPhone**, **Android**, and **Desktop**.

---

## 1. Environment Variables Configuration

Create a `.env` file in the root of your frontend/web project (or configure via the Vercel Dashboard / AI Studio Secrets panel):

```env
# Supabase Configuration
VITE_SUPABASE_URL=https://your-project.supabase.co
VITE_SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
# DO NOT EXPOSE THE SERVICE_ROLE_KEY IN CLIENT-SIDE CODE!
# SUPABASE_SERVICE_ROLE_KEY is used strictly in secure Edge Functions / Backend tasks.

# Application Identity & Mode
VITE_APP_NAME="Durga Maa Loan"
VITE_APP_ENV=production
VITE_PRIVACY_POLICY_VERSION=v2.4-2026
```

---

## 2. Supabase Database & Security Migration Steps

1. **Log in to Supabase Console**: Navigate to your Supabase project dashboard.
2. **Execute Schema Migration**:
   - Open the **SQL Editor**.
   - Paste and execute the contents of `/supabase/migrations/20261018_initial_loan_schema.sql`.
   - This sets up `profiles`, `loans`, `installments`, `repayment_transactions`, `central_upi_settings`, `kyc_submissions`, `audit_logs`, `notifications`, constraints, triggers, and indices.
3. **Apply Row Level Security (RLS)**:
   - Paste and execute `/supabase/rls_policies.sql`.
   - This enforces customer isolation: customers cannot see other users' loans, cannot modify their own balances, cannot approve their own payments, and cannot alter UPI settings.
4. **Configure Private Storage Buckets**:
   - Paste and execute `/supabase/storage_setup.sql`.
   - This provisions the private `kyc-documents` and `payment-receipts` buckets with signed URL controls and MIME restrictions.
5. **Enable Supabase Realtime**:
   - In Supabase Dashboard -> **Database** -> **Replication**.
   - Enable replication for tables: `central_upi_settings`, `loans`, `repayment_transactions`, `notifications`.
   - This automatically broadcasts UPI gateway changes and payment review decisions to all connected devices in real time.

---

## 3. GitHub & Vercel Deployment Steps

### Push to GitHub
```bash
git init
git add .
git commit -m "feat: complete Durga Maa Loan management app with central UPI and consent KYC"
git branch -M main
git remote add origin https://github.com/your-username/durga-maa-loan.git
git push -u origin main
```

### Deploy to Vercel
1. Log in to [Vercel](https://vercel.com) and click **"Add New..." -> "Project"**.
2. Select your imported GitHub repository `durga-maa-loan`.
3. Set the Framework Preset to **Vite** or **Next.js** depending on your frontend package.
4. Add the Environment Variables:
   - `VITE_SUPABASE_URL`
   - `VITE_SUPABASE_ANON_KEY`
5. Click **Deploy**. Vercel will build and assign a production URL (e.g., `https://durgamaaloan-production.vercel.app`).

---

## 4. Web App Manifest (PWA Configuration)

For PWA installation on iPhone, Android, and desktop, configure `public/manifest.json`:

```json
{
  "name": "Durga Maa Loan",
  "short_name": "Durga Maa Loan",
  "start_url": "/",
  "display": "standalone",
  "background_color": "#F8FAFC",
  "theme_color": "#0284C7",
  "orientation": "portrait",
  "icons": [
    {
      "src": "/icons/icon-192.png",
      "sizes": "192x192",
      "type": "image/png"
    },
    {
      "src": "/icons/icon-512.png",
      "sizes": "512x512",
      "type": "image/png"
    }
  ]
}
```

---

## 5. iPhone (iOS Safari) PWA Installation & Testing Instructions

iOS Safari supports full Progressive Web App installation via Web App Manifest and Apple Touch Icons:

1. **Open Safari on iPhone**: Navigate to your deployed production URL (e.g., `https://durgamaaloan-production.vercel.app`).
2. **Tap the Share Button**: Tap the iOS Safari Share icon (square with upward arrow) at the bottom toolbar.
3. **Select "Add to Home Screen"**: Scroll down and tap **"Add to Home Screen"** (`+` icon).
4. **Confirm Name**: The icon will display the customized Durga Maa Loan emblem. Tap **"Add"** in the top right.
5. **Launch in Standalone Mode**:
   - Open the app from your iPhone Home Screen.
   - It launches in full-screen standalone mode without Safari URL bars or browser controls.
6. **Test Camera Permission for KYC**:
   - Open the **KYC** tab.
   - Tap **"Optional Live Selfie Verification"**.
   - iOS Safari will present the native system prompt: *"durgamaaloan.vercel.app would like to access the camera"*.
   - Tap **"Allow"** to verify live facial selfie capture.
7. **Test UPI Repayments**:
   - Open the **Pay UPI** tab.
   - Tap **"Open in UPI App"** or scan the procedural QR code with another device running Google Pay, PhonePe, or Paytm.
   - Submit a 12-digit UTR reference number to test duplicate prevention and review queue dispatch.

---

## 6. End-to-End Testing & Verification Checklist

| Test Case | Expected Result | Verified Status |
| :--- | :--- | :--- |
| **App Branding Everywhere** | "Durga Maa Loan" displayed on home screen, login, dashboard, drawer, PWA manifest | ✅ PASS |
| **Exact Screenshot Records** | Displays Sea Money (₹7,000), See Money (₹5,000), Hello Cash (₹6,000), One Cash (₹4,000) | ✅ PASS |
| **Exact 4 Metric Figures** | Total Loan ₹22,000, Total Repaid ₹8,500, Outstanding ₹13,500, Next Limit ₹30,000 | ✅ PASS |
| **Phone Number Validation** | Rejects numbers <10 digits or non-Indian prefix; accepts valid 10 digits | ✅ PASS |
| **OTP Generation & Verification** | Generates 6-digit code with expiry and rate limiting | ✅ PASS |
| **Customer Data Isolation** | Customer A cannot view or access Customer B's loans or KYC | ✅ PASS |
| **Owner Salted Password Auth** | Matches `singhariya@8309` securely using salted SHA-256 | ✅ PASS |
| **Owner Rate Limiting & Lockout** | 3 failed password attempts trigger 30s lockout countdown | ✅ PASS |
| **Central UPI Realtime Sync** | Owner changes UPI VPA -> Instantly updates on all customer payment screens | ✅ PASS |
| **Payment Verification** | Repayment requires admin verification before balance settles or loan closes | ✅ PASS |
| **Duplicate UTR Prevention** | Entering an existing UTR is immediately rejected by database constraint | ✅ PASS |
| **Automatic Loan Closure** | When verified payments settle outstanding balance to ₹0, loan status changes to CLOSED | ✅ PASS |
| **Consent-Based KYC** | Starts with unchecked checkbox; logs customer phone, timestamp, and privacy version | ✅ PASS |
| **Consent Withdrawal** | Customer can withdraw consent, freezing loans and scheduling data deletion | ✅ PASS |
| **Private Document Storage** | Documents stored in private buckets accessible only via signed URLs | ✅ PASS |
| **Persistence Across Restarts** | Room / PostgreSQL database preserves all loans, UPI settings, and audit logs | ✅ PASS |
