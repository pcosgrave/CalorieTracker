# Security And Privacy

This document describes the current data-handling model for `BiteWise`.

## Summary

`BiteWise` currently uses a **server-readable** cloud sync model with stronger infrastructure protections:

- data is transmitted over HTTPS
- data stored in AWS is encrypted at rest
- application tables and logs use a customer-managed AWS KMS key
- user data is partitioned per user in the backend
- the backend can still read stored user data when authorized to do so

This is **not** an end-to-end encrypted or zero-knowledge design.

## Data In Transit

Cloud sync traffic is sent over HTTPS through API Gateway.

That includes:

- sign-in and token-backed API requests
- sync push and pull requests
- cloud food lookup requests

## Data At Rest

User data stored in AWS is encrypted at rest.

Current Terraform-backed protections include:

- DynamoDB tables encrypted with a **customer-managed AWS KMS key**
- S3 events bucket encrypted with the same customer-managed KMS key
- Lambda CloudWatch log groups encrypted with the same customer-managed KMS key
- KMS key rotation enabled

The current application tables include:

- `products`
- `barcode_aliases`
- `diary_entries`
- `weight_entries`
- `sync_changes`

AWS services use strong service-managed encryption internally, and this setup is intended to satisfy a practical **AES-256 at-rest** requirement through AWS KMS-backed storage encryption.

## User Separation

Cloud-synced user records are stored per user.

The primary partitioning key is the authenticated Cognito subject:

- user-owned rows are written with `ownerUserId = <cognito-sub>`
- sync pull queries return only rows for the authenticated `ownerUserId`
- account deletion removes only rows owned by that user

Community food records are stored separately under:

- `ownerUserId = "__community__"`

That means deleting a user account does **not** remove community records.

## Operator Access

The current design is **server-readable**.

That means:

- AWS operators with sufficient IAM permissions can read stored application data
- DynamoDB encryption at rest does **not** prevent an authorized operator from reading the decrypted data through AWS APIs or the console

This is an intentional tradeoff in the current architecture because the backend performs:

- sync reconciliation
- canonical table materialization
- community record publishing
- conflict handling

## Logging

Lambda log groups are pre-created with:

- retention period controls
- customer-managed KMS encryption

Even so, logs should still be treated carefully. The safest posture is to avoid writing raw user payloads into logs unless actively debugging a problem.

## Sync Model

`BiteWise` uses an offline-first sync model.

The current design is:

- client saves locally first
- client pushes change events to the backend
- backend reconciles changes against current server state
- backend materializes accepted changes into canonical tables
- backend stores sync events in `sync_changes` for pull replay and audit

When two devices conflict, older offline changes should not overwrite newer server values.

## Current Privacy Model

The current model is best described as:

- **stronger server-readable model**

It is stronger than a default cloud setup because it uses:

- customer-managed KMS
- scoped IAM permissions
- encrypted logs

But it is not the same as:

- end-to-end encryption
- zero-knowledge storage

## What This Model Is Good For

This model is a good fit when you want:

- practical infrastructure security
- offline-first sync
- server-side reconciliation
- community/shared food records
- simpler operations than a zero-knowledge architecture

## What It Does Not Guarantee

This model does **not** guarantee:

- that the backend owner cannot read data
- that stored payloads are unreadable to all operators
- that server-side search/materialization can happen without access to plaintext

Those properties would require an end-to-end encrypted design with very different key management and sync tradeoffs.

