# Partner Examples

This folder contains partner API examples using the OpenAI Java SDK.

## Prerequisites

1. Install dependencies:

   ```bash
   mvn install -DskipTests
   ```

2. Configure constants in each example file:
   - `COMPARTMENT_ID`
   - `REGION`

## Notes

- Partner endpoints use pass-through mode and require the `opc-compartment-id` header.
- These examples use IAM signing through `oci-genai-auth-java`.
