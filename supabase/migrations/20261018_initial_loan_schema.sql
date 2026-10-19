-- ====================================================================
-- DURGA MAA LOAN POSTGRESQL SCHEMA MIGRATION: 20261018_initial_loan_schema.sql
-- RBI Compliant Loan Management, Central UPI Gateway & Consent KYC
-- ====================================================================

-- 1. Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 2. Enumerated Types
CREATE TYPE user_role AS ENUM ('CUSTOMER', 'OWNER');
CREATE TYPE kyc_status AS ENUM ('NOT_SUBMITTED', 'PENDING_REVIEW', 'APPROVED', 'REJECTED');
CREATE TYPE loan_status AS ENUM ('PENDING_APPROVAL', 'ACTIVE', 'CLOSED', 'REJECTED');
CREATE TYPE installment_status AS ENUM ('UPCOMING', 'PAID', 'OVERDUE');
CREATE TYPE payment_status AS ENUM ('PENDING_VERIFICATION', 'VERIFIED', 'REJECTED');
CREATE TYPE document_type AS ENUM ('AADHAAR_CARD', 'PAN_CARD', 'PASSPORT', 'VOTER_ID');

-- 3. PROFILES TABLE (Linked to Supabase Auth users)
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    phone VARCHAR(10) NOT NULL UNIQUE CHECK (phone ~ '^[6-9][0-9]{9}$'),
    full_name TEXT NOT NULL,
    email TEXT,
    role user_role DEFAULT 'CUSTOMER'::user_role NOT NULL,
    kyc_status kyc_status DEFAULT 'NOT_SUBMITTED'::kyc_status NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- 4. CENTRAL UPI SETTINGS TABLE (Single Authoritative Record)
CREATE TABLE IF NOT EXISTS public.central_upi_settings (
    id INT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    upi_id TEXT NOT NULL CHECK (position('@' in upi_id) > 1),
    receiver_name TEXT NOT NULL,
    bank_name TEXT NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_by TEXT DEFAULT 'Authorized Administrator' NOT NULL
);

-- Seed initial Central UPI configuration
INSERT INTO public.central_upi_settings (id, upi_id, receiver_name, bank_name)
VALUES (1, 'nr853950-1@okicici', 'Durga Maa Loan Financial Services', 'ICICI Bank Ltd')
ON CONFLICT (id) DO NOTHING;

-- 5. LOANS TABLE
CREATE TABLE IF NOT EXISTS public.loans (
    id VARCHAR(32) PRIMARY KEY, -- e.g. 'LN-10842'
    customer_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE RESTRICT,
    customer_phone VARCHAR(10) NOT NULL,
    loan_type TEXT NOT NULL,
    principal_amount NUMERIC(12, 2) NOT NULL CHECK (principal_amount >= 5000),
    annual_interest_rate NUMERIC(5, 2) NOT NULL CHECK (annual_interest_rate > 0),
    tenure_months INT NOT NULL CHECK (tenure_months BETWEEN 3 AND 60),
    total_repayment_amount NUMERIC(12, 2) NOT NULL,
    balance_remaining NUMERIC(12, 2) NOT NULL CHECK (balance_remaining >= 0),
    monthly_emi NUMERIC(12, 2) NOT NULL CHECK (monthly_emi > 0),
    status loan_status DEFAULT 'PENDING_APPROVAL'::loan_status NOT NULL,
    purpose TEXT NOT NULL,
    applied_date TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    approved_date TIMESTAMPTZ,
    closed_date TIMESTAMPTZ,
    next_due_date TIMESTAMPTZ
);

CREATE INDEX idx_loans_customer_id ON public.loans(customer_id);
CREATE INDEX idx_loans_customer_phone ON public.loans(customer_phone);
CREATE INDEX idx_loans_status ON public.loans(status);

-- 6. INSTALLMENTS SCHEDULE TABLE
CREATE TABLE IF NOT EXISTS public.installments (
    id VARCHAR(48) PRIMARY KEY, -- e.g. 'INS-LN-10842-1'
    loan_id VARCHAR(32) NOT NULL REFERENCES public.loans(id) ON DELETE CASCADE,
    installment_number INT NOT NULL,
    due_date TIMESTAMPTZ NOT NULL,
    emi_amount NUMERIC(12, 2) NOT NULL,
    principal_component NUMERIC(12, 2) NOT NULL,
    interest_component NUMERIC(12, 2) NOT NULL,
    status installment_status DEFAULT 'UPCOMING'::installment_status NOT NULL,
    penalty_amount NUMERIC(10, 2) DEFAULT 0.0 NOT NULL,
    paid_date TIMESTAMPTZ,
    transaction_ref TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

CREATE INDEX idx_installments_loan_id ON public.installments(loan_id);

-- 7. REPAYMENT TRANSACTIONS TABLE
CREATE TABLE IF NOT EXISTS public.repayment_transactions (
    id VARCHAR(32) PRIMARY KEY, -- e.g. 'TXN-74921'
    loan_id VARCHAR(32) NOT NULL REFERENCES public.loans(id) ON DELETE RESTRICT,
    installment_id VARCHAR(48) REFERENCES public.installments(id),
    customer_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE RESTRICT,
    customer_phone VARCHAR(10) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    utr_number VARCHAR(32) NOT NULL UNIQUE, -- Duplicate UTR prevention enforced at DB level
    payment_method TEXT DEFAULT 'UPI' NOT NULL,
    receipt_storage_path TEXT,
    status payment_status DEFAULT 'PENDING_VERIFICATION'::payment_status NOT NULL,
    submitted_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    verified_at TIMESTAMPTZ,
    verified_by TEXT,
    admin_notes TEXT
);

CREATE INDEX idx_repayments_loan_id ON public.repayment_transactions(loan_id);
CREATE INDEX idx_repayments_customer_phone ON public.repayment_transactions(customer_phone);
CREATE UNIQUE INDEX idx_repayments_utr ON public.repayment_transactions(utr_number);

-- 8. CONSENT-BASED KYC SUBMISSIONS TABLE
CREATE TABLE IF NOT EXISTS public.kyc_submissions (
    id VARCHAR(32) PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    customer_phone VARCHAR(10) NOT NULL,
    document_type document_type NOT NULL,
    id_number TEXT NOT NULL,
    document_storage_path TEXT,
    selfie_storage_path TEXT,
    consent_given BOOLEAN NOT NULL CHECK (consent_given = TRUE),
    consent_timestamp TIMESTAMPTZ NOT NULL,
    consent_purpose TEXT DEFAULT 'Identity verification and statutory loan eligibility assessment' NOT NULL,
    privacy_policy_version VARCHAR(16) DEFAULT 'v2.4-2026' NOT NULL,
    status kyc_status DEFAULT 'PENDING_REVIEW'::kyc_status NOT NULL,
    submitted_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    reviewed_at TIMESTAMPTZ,
    reviewed_by TEXT,
    review_notes TEXT,
    consent_withdrawn BOOLEAN DEFAULT FALSE NOT NULL,
    consent_withdrawn_at TIMESTAMPTZ
);

CREATE INDEX idx_kyc_customer_id ON public.kyc_submissions(customer_id);

-- 9. IMMUTABLE AUDIT LOGS TABLE
CREATE TABLE IF NOT EXISTS public.audit_logs (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    actor TEXT NOT NULL,
    action TEXT NOT NULL,
    details TEXT NOT NULL,
    ip_or_origin TEXT DEFAULT 'Client / Applet' NOT NULL
);

CREATE INDEX idx_audit_logs_timestamp ON public.audit_logs(timestamp DESC);

-- 10. NOTIFICATIONS TABLE
CREATE TABLE IF NOT EXISTS public.notifications (
    id BIGSERIAL PRIMARY KEY,
    recipient_phone VARCHAR(16) NOT NULL,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    notification_type TEXT NOT NULL,
    timestamp TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    is_read BOOLEAN DEFAULT FALSE NOT NULL,
    reference_id TEXT
);

CREATE INDEX idx_notifications_recipient ON public.notifications(recipient_phone);

-- 11. TRIGGER: Automatic Loan Balance Settlement on Verified Repayment
CREATE OR REPLACE FUNCTION public.handle_payment_verification()
RETURNS TRIGGER AS $$
DECLARE
    v_loan_balance NUMERIC;
    v_new_balance NUMERIC;
BEGIN
    IF NEW.status = 'VERIFIED' AND OLD.status = 'PENDING_VERIFICATION' THEN
        -- Lock and fetch current balance
        SELECT balance_remaining INTO v_loan_balance FROM public.loans WHERE id = NEW.loan_id FOR UPDATE;
        v_new_balance := GREATEST(0.0, v_loan_balance - NEW.amount);

        -- Update loan balance
        UPDATE public.loans
        SET balance_remaining = v_new_balance,
            status = CASE WHEN v_new_balance <= 0 THEN 'CLOSED'::loan_status ELSE status END,
            closed_date = CASE WHEN v_new_balance <= 0 THEN NOW() ELSE closed_date END
        WHERE id = NEW.loan_id;

        -- Record in audit log
        INSERT INTO public.audit_logs (actor, action, details)
        VALUES (
            COALESCE(NEW.verified_by, 'Authorized Administrator'),
            'PAYMENT_VERIFIED_TRIGGER',
            format('Settled %s for loan %s (UTR %s). New balance: %s', NEW.amount, NEW.loan_id, NEW.utr_number, v_new_balance)
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE OR REPLACE TRIGGER trg_on_payment_verified
AFTER UPDATE OF status ON public.repayment_transactions
FOR EACH ROW
EXECUTE FUNCTION public.handle_payment_verification();
