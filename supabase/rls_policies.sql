-- ====================================================================
-- DURGA MAA LOAN ROW LEVEL SECURITY (RLS) POLICIES
-- Zero-Trust Multi-Tenant Isolation & Owner Administration Controls
-- ====================================================================

-- 1. Enable RLS on all tables
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.loans ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.installments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.repayment_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.central_upi_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.kyc_submissions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;

-- Helper function to check if current authenticated user is an Owner
CREATE OR REPLACE FUNCTION public.is_owner()
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM public.profiles
        WHERE id = auth.uid() AND role = 'OWNER'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 2. PROFILES POLICIES
CREATE POLICY "Users can view own profile"
ON public.profiles FOR SELECT
USING (auth.uid() = id OR public.is_owner());

CREATE POLICY "Users can update own non-sensitive profile"
ON public.profiles FOR UPDATE
USING (auth.uid() = id)
WITH CHECK (
    auth.uid() = id AND
    role = (SELECT role FROM public.profiles WHERE id = auth.uid()) -- Prevents self-elevation to OWNER
);

-- 3. CENTRAL UPI SETTINGS POLICIES
-- All authenticated users can read the active central UPI settings (needed for repayment screen)
CREATE POLICY "Public read central upi"
ON public.central_upi_settings FOR SELECT
TO authenticated
USING (true);

-- ONLY authorized Owner can update or insert UPI settings
CREATE POLICY "Owner only can update upi settings"
ON public.central_upi_settings FOR UPDATE
TO authenticated
USING (public.is_owner())
WITH CHECK (public.is_owner());

-- 4. LOANS POLICIES
-- Customers can view only their own loans; Owners can view all
CREATE POLICY "Customer can view own loans"
ON public.loans FOR SELECT
USING (customer_id = auth.uid() OR public.is_owner());

-- Customers can submit loan applications (status must default to PENDING_APPROVAL)
CREATE POLICY "Customer can apply for loan"
ON public.loans FOR INSERT
WITH CHECK (
    customer_id = auth.uid() AND
    status = 'PENDING_APPROVAL'::loan_status
);

-- Only Owners can approve/reject loans or alter balances
CREATE POLICY "Owner can update loan status"
ON public.loans FOR UPDATE
USING (public.is_owner())
WITH CHECK (public.is_owner());

-- 5. INSTALLMENTS POLICIES
CREATE POLICY "Customer can view installments of own loans"
ON public.installments FOR SELECT
USING (
    EXISTS (
        SELECT 1 FROM public.loans
        WHERE loans.id = installments.loan_id
        AND (loans.customer_id = auth.uid() OR public.is_owner())
    )
);

CREATE POLICY "Owner can manage installments"
ON public.installments FOR ALL
USING (public.is_owner());

-- 6. REPAYMENT TRANSACTIONS POLICIES
-- Customers can view their own repayment submissions; Owners can view all
CREATE POLICY "Customer can view own repayments"
ON public.repayment_transactions FOR SELECT
USING (customer_id = auth.uid() OR public.is_owner());

-- Customers can insert new repayment submissions (must start as PENDING_VERIFICATION)
CREATE POLICY "Customer can submit repayment"
ON public.repayment_transactions FOR INSERT
WITH CHECK (
    customer_id = auth.uid() AND
    status = 'PENDING_VERIFICATION'::payment_status
);

-- Customers CANNOT verify payments. Only Owner can update payment status.
CREATE POLICY "Owner only can verify repayments"
ON public.repayment_transactions FOR UPDATE
USING (public.is_owner())
WITH CHECK (public.is_owner());

-- 7. KYC SUBMISSIONS POLICIES
CREATE POLICY "Customer can view own kyc"
ON public.kyc_submissions FOR SELECT
USING (customer_id = auth.uid() OR public.is_owner());

CREATE POLICY "Customer can submit kyc with consent"
ON public.kyc_submissions FOR INSERT
WITH CHECK (
    customer_id = auth.uid() AND
    consent_given = TRUE AND
    status = 'PENDING_REVIEW'::kyc_status
);

CREATE POLICY "Owner can review kyc"
ON public.kyc_submissions FOR UPDATE
USING (public.is_owner())
WITH CHECK (public.is_owner());

-- 8. AUDIT LOGS POLICIES
-- Only Owners can view audit logs; append-only
CREATE POLICY "Owner can view audit logs"
ON public.audit_logs FOR SELECT
USING (public.is_owner());

CREATE POLICY "Allow server insertion into audit logs"
ON public.audit_logs FOR INSERT
WITH CHECK (true);

-- 9. NOTIFICATIONS POLICIES
CREATE POLICY "Customer can view own notifications"
ON public.notifications FOR SELECT
USING (
    recipient_phone = (SELECT phone FROM public.profiles WHERE id = auth.uid()) OR
    (public.is_owner() AND recipient_phone = 'OWNER') OR
    recipient_phone = 'ALL'
);

CREATE POLICY "Users can mark own notifications as read"
ON public.notifications FOR UPDATE
USING (
    recipient_phone = (SELECT phone FROM public.profiles WHERE id = auth.uid()) OR
    (public.is_owner() AND recipient_phone = 'OWNER')
);
