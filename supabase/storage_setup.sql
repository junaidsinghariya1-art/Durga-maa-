-- ====================================================================
-- DURGA MAA LOAN PRIVATE STORAGE SETUP & BUCKET POLICIES
-- Zero-Trust Document Storage, Signed URLs & Strict MIME Enforcement
-- ====================================================================

-- 1. Create Private Storage Buckets
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES 
    ('kyc-documents', 'kyc-documents', false, 10485760, ARRAY['image/jpeg', 'image/png', 'image/webp', 'application/pdf']),
    ('payment-receipts', 'payment-receipts', false, 10485760, ARRAY['image/jpeg', 'image/png', 'image/webp'])
ON CONFLICT (id) DO UPDATE SET
    public = false,
    file_size_limit = 10485760,
    allowed_mime_types = EXCLUDED.allowed_mime_types;

-- 2. KYC DOCUMENTS STORAGE POLICIES
-- Customers can upload only into their own folder: kyc-documents/{user_id}/*
CREATE POLICY "Customers upload own kyc documents"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (
    bucket_id = 'kyc-documents' AND
    (storage.foldername(name))[1] = auth.uid()::text
);

-- Customers can view only their own KYC files via signed URLs
CREATE POLICY "Customers view own kyc documents"
ON storage.objects FOR SELECT
TO authenticated
USING (
    bucket_id = 'kyc-documents' AND
    ((storage.foldername(name))[1] = auth.uid()::text OR public.is_owner())
);

-- 3. PAYMENT RECEIPTS STORAGE POLICIES
-- Customers can upload payment receipts into their own directory: payment-receipts/{user_id}/*
CREATE POLICY "Customers upload repayment receipts"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (
    bucket_id = 'payment-receipts' AND
    (storage.foldername(name))[1] = auth.uid()::text
);

-- Customers and Authorized Owners can view payment receipts
CREATE POLICY "Authorized view of repayment receipts"
ON storage.objects FOR SELECT
TO authenticated
USING (
    bucket_id = 'payment-receipts' AND
    ((storage.foldername(name))[1] = auth.uid()::text OR public.is_owner())
);
