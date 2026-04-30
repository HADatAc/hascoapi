# HADatAc Documentation

This directory contains the essential documentation for the HADatAc (Human-Aware Data Acquisition Framework) project.

---

## 📚 Documentation by Metadata Template

### 🎯 VSTOI-DSG (Primary Implementation)
**[VSTOI-DSG-IMPLEMENTATION-COMPLETE.md](VSTOI-DSG-IMPLEMENTATION-COMPLETE.md)**
- Complete technical documentation for VSTOI-based DSG architecture
- Replaces legacy INS file system
- Covers all 7 VSTOI entity types
- **PRIMARY DOCUMENTATION FOR INStoDSG BRANCH**

**[VSTOI-DEPENDENCY-RESOLUTION-ANALYSIS.md](VSTOI-DEPENDENCY-RESOLUTION-ANALYSIS.md)**
- VSTOI entity dependency resolution fix
- Handles both `prefix:name` and `prefix:/name` URI formats
- Global fix in URIUtils.replacePrefixEx()

---

### 📋 Specifications

**[DASOC-SPECIFICATION-v1.1.md](DASOC-SPECIFICATION-v1.1.md)**
- DA-SOC file format specification
- VSTOI entity enrichment workflows

**[SDD-SPECIFICATION-IMPROVED.md](SDD-SPECIFICATION-IMPROVED.md)**
- Semantic Data Dictionary specification
- Attribute and entity definitions

**[WKF-TECHNICAL-ARCHITECTURE-EN.md](WKF-TECHNICAL-ARCHITECTURE-EN.md)**
- Workflow (WKF) technical architecture
- Generation and ingestion workflows

**[DP2-COMPLETE-SOLUTION.md](DP2-COMPLETE-SOLUTION.md)**
- Deployment Plan 2 (DP2) complete solution
- Platform instance management

---

### 🔧 Operational Documentation

**[API-DOCUMENTATION.md](API-DOCUMENTATION.md)**
- Complete API endpoint reference
- Request/response formats

**[QUICK-TEST-GUIDE.md](QUICK-TEST-GUIDE.md)**
- Testing procedures for all MTs
- Validation workflows

**[USER-GUIDE-NON-TECHNICAL.md](USER-GUIDE-NON-TECHNICAL.md)**
- User guide for non-technical users
- Step-by-step workflows

---

## 🚀 Quick Start

### New Users
1. Read **[VSTOI-DSG-IMPLEMENTATION-COMPLETE.md](VSTOI-DSG-IMPLEMENTATION-COMPLETE.md)** for architecture overview
2. Check **[USER-GUIDE-NON-TECHNICAL.md](USER-GUIDE-NON-TECHNICAL.md)** for workflows
3. Use **[QUICK-TEST-GUIDE.md](QUICK-TEST-GUIDE.md)** for testing

### Developers
1. Study **[VSTOI-DSG-IMPLEMENTATION-COMPLETE.md](VSTOI-DSG-IMPLEMENTATION-COMPLETE.md)** for implementation details
2. Reference **[API-DOCUMENTATION.md](API-DOCUMENTATION.md)** for endpoints
3. Review **[WKF-TECHNICAL-ARCHITECTURE-EN.md](WKF-TECHNICAL-ARCHITECTURE-EN.md)** for WKF architecture

### Data Managers
1. Start with **[DASOC-SPECIFICATION-v1.1.md](DASOC-SPECIFICATION-v1.1.md)** for DA-SOC format
2. Follow **[QUICK-TEST-GUIDE.md](QUICK-TEST-GUIDE.md)** for testing procedures
3. Consult **[SDD-SPECIFICATION-IMPROVED.md](SDD-SPECIFICATION-IMPROVED.md)** for semantic definitions

---

## 📖 Document Index

| Document | Purpose | MT Type |
|----------|---------|---------|
| VSTOI-DSG-IMPLEMENTATION-COMPLETE | Complete VSTOI-DSG implementation | DSG |
| DASOC-SPECIFICATION-v1.1 | DA-SOC file format | DA-SOC |
| SDD-SPECIFICATION-IMPROVED | Semantic Data Dictionary | SDD |
| WKF-TECHNICAL-ARCHITECTURE-EN | Workflow architecture | WKF |
| DP2-COMPLETE-SOLUTION | Deployment Plan 2 | DP2 |
| API-DOCUMENTATION | API reference | - |
| QUICK-TEST-GUIDE | Testing procedures | All |
| USER-GUIDE-NON-TECHNICAL | User workflows | All |

---

**Last Updated**: April 30, 2026  
**Maintainer**: Kaell  
**Total Documents**: 10 (1 per MT + guides)

