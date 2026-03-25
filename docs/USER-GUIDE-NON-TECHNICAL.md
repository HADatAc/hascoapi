# HAScO System - User Guide for Non-Technical Users

**Version:** 1.0  
**Last Updated:** February 14, 2026  
**Audience:** Business users, researchers, administrators without IT background

---

## Table of Contents

1. [What is HAScO?](#what-is-hasco)
2. [Key Concepts Explained Simply](#key-concepts-explained-simply)
3. [How the System Works](#how-the-system-works)
4. [Working with Metadata Templates](#working-with-metadata-templates)
5. [Understanding the Workflow](#understanding-the-workflow)
6. [Common Tasks](#common-tasks)
7. [Troubleshooting Common Issues](#troubleshooting-common-issues)
8. [Glossary](#glossary)

---

## What is HAScO?

HAScO (Human-Aware Science Ontology) is a system that helps you **organize and manage scientific data** in a structured way. Think of it as a sophisticated filing cabinet where you can:

- **Store information** about scientific instruments, experiments, and observations
- **Organize data** using standardized templates (like forms you fill out)
- **Share information** with other researchers in a way everyone can understand
- **Track relationships** between different pieces of information

### Real-World Analogy

Imagine you're organizing a large library:
- **Books** = Your scientific data
- **Card catalog** = The HAScO system
- **Classification system** = The templates and rules
- **Librarian** = The system helping you find and organize everything

---

## Key Concepts Explained Simply

### 1. **Metadata Template (MT)**

Think of a metadata template as a **structured form** or **questionnaire** that helps you describe something in a consistent way.

**Example:** If you're describing a medical simulator:
- What is its name?
- What type is it?
- What instruments does it include?
- Who manages it?

Just like filling out a medical form, you fill out these templates with specific information.

### 2. **Types of Templates**

The system uses different types of templates for different purposes:

| Template Type | What It Does | Real-World Example |
|---------------|--------------|-------------------|
| **WKF** (Workflow) | Describes a series of steps or procedures | A medical training protocol with 5 steps |
| **INS** (Instruments) | Describes equipment and tools | A blood pressure monitor, a stethoscope |
| **DP2** (Deployment) | Describes how equipment is used | Which simulator is in which training room |
| **SDD** (Data Dictionary) | Defines what data fields mean | "Temperature" means body temperature in Celsius |

### 3. **Ingestion**

**Ingestion** is the process of **importing your data into the system**. Think of it like:
- Scanning documents into a digital filing system
- Uploading photos to a cloud storage
- Entering information from a paper form into a database

**What happens during ingestion?**
1. You upload a file (usually an Excel spreadsheet)
2. The system reads and validates the information
3. The system stores it in an organized way
4. You can now search, view, and use this information

### 4. **Generation**

**Generation** is the opposite of ingestion - it's **creating a template file from existing data**.

**Why is this useful?**
- You want to update existing information
- You need to create a similar template with slight changes
- You want to export data to share with others

**Real-World Analogy:** Imagine you filled out a form last year. Generation is like the system creating a new copy of that form with your information already filled in, so you can just update what changed.

---

## How the System Works

### The Three Main Components

```
┌─────────────────┐
│   Frontend      │  ← What you see (web interface)
│  (Drupal Site)  │     - Forms to fill out
└────────┬────────┘     - Lists of your data
         │              - Buttons to click
         │
         ↓
┌─────────────────┐
│   Backend       │  ← The processing engine
│  (HAScO API)    │     - Validates your data
└────────┬────────┘     - Stores information
         │              - Performs actions
         │
         ↓
┌─────────────────┐
│  Data Storage   │  ← Where information lives
│   (Fuseki DB)   │     - Organized storage
└─────────────────┘     - Searchable database
```

### Step-by-Step: What Happens When You Upload Data

Let's follow a **real example** - uploading information about a medical training workflow:

#### **Step 1: You Fill Out a Form**
- You go to the website
- Click "Add New Workflow"
- Fill in details:
  - Name: "CPR Training Protocol"
  - Description: "Basic CPR steps"
  - Version: 1

#### **Step 2: You Upload a File**
- You attach an Excel file with detailed steps
- The file has sheets like:
  - InfoSheet (general information)
  - Processes (each training step)
  - Tasks (specific actions)

#### **Step 3: System Validates**
- Checks if required fields are filled
- Verifies the file format is correct
- Looks for any errors

#### **Step 4: System Stores**
- If everything is correct, it saves your data
- Creates connections (e.g., linking tasks to processes)
- Makes it searchable

#### **Step 5: You Can Use It**
- View the workflow in the list
- Generate a new version
- Share with colleagues
- Delete if no longer needed

---

## Working with Metadata Templates

### How to Create a New Template

#### For Workflows (WKF):

1. **Navigate:** Go to "Workflows" → "Add New Workflow"
2. **Fill Basic Info:**
   - Name: Give it a clear, descriptive name
   - Comment: Brief description
3. **Prepare Your Excel File:**
   - Download the template file (if available)
   - Fill in the sheets:
     - **InfoSheet:** Overview information
     - **Processes:** Main steps
     - **Tasks:** Detailed actions
4. **Upload:** Click "Choose File" and select your Excel
5. **Submit:** Click "Create"
6. **Ingest:** After creation, click "Ingest" to process the data

#### For Instruments (INS):

1. **Navigate:** Go to "Instruments" → "Add New Instrument"
2. **Fill Basic Info:**
   - Name: Instrument name
   - Comment: What it's used for
3. **Prepare Your Excel File:**
   - **Instruments sheet:** List of equipment
   - **Components sheet:** Parts of each instrument
   - **CodeBooks sheet:** Standard response options
4. **Upload and Submit**
5. **Ingest**

### Understanding Template Status

Your templates go through different states:

| Status | What It Means | What You Can Do |
|--------|---------------|-----------------|
| **UNPROCESSED** | File uploaded but not yet imported | Wait or click "Ingest" |
| **WORKING** | System is currently processing | Wait (usually takes seconds) |
| **PROCESSED** | Successfully imported | View, Generate, or Delete |
| **DRAFT** | Partial or incomplete | Complete and re-ingest |

---

## Understanding the Workflow

### The Complete Lifecycle

```
1. CREATE
   └─→ Fill form + upload file
       
2. VALIDATE
   └─→ System checks for errors
       
3. INGEST
   └─→ Data is imported and stored
       
4. USE
   ├─→ View in lists
   ├─→ Search
   └─→ Generate new versions
       
5. UPDATE (if needed)
   └─→ Generate → Modify → Re-ingest
       
6. DELETE (if no longer needed)
   └─→ Uningest → Delete
```

### Example: Complete Workflow Lifecycle

**Scenario:** You're documenting a new medical training simulator.

#### **Week 1: Initial Creation**
- Create an Instrument template (INS)
- Upload Excel with all simulator components
- Ingest → Status: PROCESSED

#### **Week 4: Create Training Protocol**
- Create a Workflow template (WKF)
- Reference the instruments from your INS
- Describe training steps
- Ingest → Status: PROCESSED

#### **Week 8: Deploy to Training Room**
- Create a Deployment template (DP2)
- Specify which simulator is in which room
- Link to your INS and WKF
- Generate to create the file
- System creates Excel with all the information

#### **Week 20: Update Protocol**
- Generate a new version of WKF
- Download the generated Excel
- Modify steps in Excel
- Upload as version 2
- Re-ingest

#### **Week 50: Equipment Retired**
- Uningest the DP2 (removes deployment info)
- Delete the DP2 template
- Keep INS and WKF for historical records

---

## Common Tasks

### Task 1: Viewing Your Templates

**Where to look:**
- Main menu → Select template type (WKF, INS, DP2, etc.)
- You'll see a table with:
  - **Name:** What you called it
  - **Version:** Current version number
  - **Status:** Processing state
  - **Actions:** Buttons for Ingest, Generate, Delete

**What the columns mean:**
- **Label:** The name you gave it
- **Comment:** Your description
- **Status:** Current state (UNPROCESSED, PROCESSED, etc.)
- **Version:** Helps track changes over time
- **Manager Email:** Who created/owns it

### Task 2: Ingesting a Template

**When to do this:** After uploading a new file or updating an existing template.

**Steps:**
1. Find your template in the list
2. Check status = UNPROCESSED or WORKING
3. Click the "Ingest" button
4. Wait a few seconds
5. Refresh the page
6. Status should now be PROCESSED

**If it fails:**
- Check the error message
- Common issues:
  - Missing required fields in Excel
  - Wrong file format
  - Invalid references

### Task 3: Generating a Template

**When to do this:** When you want to export or update existing data.

**Steps:**
1. Find your template in the list
2. Status must be PROCESSED
3. Click "Generate"
4. System creates an Excel file
5. Download the file
6. You can now:
   - Review the data
   - Modify and re-upload
   - Share with colleagues

### Task 4: Deleting a Template

**Important:** Deleting is permanent! Make sure you really want to remove it.

**Steps:**
1. First, "Uningest" if status is PROCESSED
   - This removes the data from the system
   - Status becomes UNPROCESSED
2. Then, click "Delete"
   - This removes the template record
3. Confirm the deletion

**What gets deleted:**
- The template record
- Associated file
- All data that was ingested

**What remains:**
- Historical logs (in some cases)
- Related templates (they won't break, but may have missing references)

### Task 5: Searching for Templates

**How to search:**
- Use the search box at the top of template lists
- Search by:
  - Name
  - Comment
  - Manager email
  - Version

**Tips:**
- Use specific keywords
- Check spelling
- Try partial names (e.g., "CPR" finds "CPR Training Protocol")

---

## Troubleshooting Common Issues

### Problem 1: "File Not Found" During Ingestion

**What it means:** The system can't find the file you uploaded.

**Why it happens:**
- File was uploaded but didn't save properly
- Server storage issue
- Network interruption during upload

**How to fix:**
1. Delete the template
2. Re-create it
3. Upload the file again
4. Make sure you see a confirmation message
5. Try ingesting again

---

### Problem 2: "Template Status Stuck on WORKING"

**What it means:** The system is processing but seems frozen.

**Why it happens:**
- Large file taking time
- Server is busy
- Processing error

**How to fix:**
1. Wait 2-3 minutes
2. Refresh the page
3. If still WORKING after 5 minutes:
   - Note the template name
   - Contact system administrator
   - Provide template details

---

### Problem 3: "Generation Produces Empty File"

**What it means:** The generated Excel has headers but no data.

**Why it happens:**
- Template not ingested
- Data wasn't saved correctly
- Wrong template type selected

**How to fix:**
1. Check status = PROCESSED
2. If not, ingest first
3. Try generating again
4. If still empty, re-ingest the original file

---

### Problem 4: "Cannot Delete Template"

**What it means:** Delete button doesn't work or shows error.

**Why it happens:**
- Template is still PROCESSED (must uningest first)
- Template is referenced by other templates
- Permission issue

**How to fix:**
1. First, click "Uningest"
2. Wait for status to become UNPROCESSED
3. Then click "Delete"
4. If still fails, contact administrator

---

### Problem 5: "Invalid File Format"

**What it means:** The Excel file you uploaded has errors.

**Why it happens:**
- Wrong sheet names
- Missing required columns
- Invalid data in cells
- File is corrupted

**How to fix:**
1. Download the template example (if available)
2. Compare your file to the example
3. Check:
   - Sheet names are exact (case-sensitive)
   - All required columns exist
   - Data types are correct (numbers vs. text)
4. Save a clean copy
5. Upload again

---

## Glossary

**Simple definitions of terms you'll encounter:**

### A-D

**API (Backend)**
- The "engine" of the system that does the work behind the scenes
- You don't see it, but it processes your requests

**Deployment (DP2)**
- Information about where equipment is located and how it's being used
- Example: "Simulator A is in Training Room 3"

**Draft Status**
- A template that's incomplete or being worked on
- Not yet ready for use

### E-I

**Excel File**
- The file format (.xlsx) used to upload bulk data
- Contains multiple sheets (tabs) with different information

**Frontend**
- The website you see and interact with
- Has forms, buttons, and lists

**Fuseki**
- The database where all information is stored
- Think of it as a massive, organized filing cabinet

**Ingestion**
- The process of importing your Excel data into the system
- Like scanning a document into digital storage

**Instrument (INS)**
- Equipment, tools, or devices used in research
- Example: A blood pressure monitor, a training mannequin

### M-P

**Metadata**
- "Data about data" - information that describes other information
- Example: For a photo, metadata = date taken, camera used, location

**Metadata Template (MT)**
- A standardized form for entering data
- Ensures everyone describes things the same way

**Processed Status**
- Data has been successfully imported and is ready to use

### S-W

**Semantic Data**
- Data organized so computers can understand relationships
- Example: "X is a type of Y" or "A is used in B"

**Status**
- The current state of your template
- Shows what's happening or what you can do next

**Triple Store**
- A type of database that stores information as relationships
- Format: Subject - Predicate - Object
- Example: "Simulator" - "is located in" - "Room 3"

**Uningest**
- Removing data from the system (opposite of ingest)
- Does NOT delete the template record, just the data

**URI (Unique Identifier)**
- A unique code that identifies each piece of information
- Like a barcode or ID number
- Example: `http://hadatac.org/ont/hadatac#/WKF1770736689677451`

**Workflow (WKF)**
- A series of steps or procedures
- Example: "CPR Training Protocol" with 10 steps

---

## Getting Help

### When You Need Assistance

**Before contacting support:**
1. Check this guide
2. Look for error messages (write them down)
3. Note what you were trying to do
4. Check the template status

**What to provide when asking for help:**
- Template name and type (WKF, INS, etc.)
- What you were trying to do
- Error message (exact text)
- Template status
- Your email (used to create the template)

### Common Questions

**Q: How long does ingestion take?**
A: Usually 5-30 seconds for most files. Large files with thousands of rows may take 1-2 minutes.

**Q: Can I edit a template after ingesting?**
A: Not directly. You must: Generate → Download → Edit in Excel → Upload new version → Re-ingest.

**Q: What file types are supported?**
A: Excel (.xlsx) for most templates. Some templates may accept CSV.

**Q: Can I undo a deletion?**
A: No, deletions are permanent. Always make backups of important files.

**Q: How many versions can I have?**
A: Unlimited. The system tracks versions automatically.

**Q: Can multiple people work on the same template?**
A: Yes, but only one person can edit at a time. Coordinate with your team.

---

## Best Practices

### Do's ✅

- **Save backups** of your Excel files before uploading
- **Use clear, descriptive names** for templates
- **Add comments** explaining what the template is for
- **Check status** before performing actions
- **Wait for processing** to complete before doing other actions
- **Test with small files** first if you're new

### Don'ts ❌

- **Don't delete** without uningesting first
- **Don't upload** multiple versions simultaneously
- **Don't use special characters** in names (stick to letters, numbers, spaces)
- **Don't close the browser** while ingesting
- **Don't skip validation** steps
- **Don't forget to ingest** after uploading

---

## Quick Reference Card

**Print this section for your desk!**

| I Want To... | Steps |
|--------------|-------|
| **Create new template** | 1. Click "Add New" <br> 2. Fill form <br> 3. Upload file <br> 4. Submit <br> 5. Ingest |
| **View my templates** | 1. Select type from menu <br> 2. Browse list |
| **Update existing** | 1. Generate <br> 2. Download <br> 3. Edit in Excel <br> 4. Create new version <br> 5. Upload <br> 6. Ingest |
| **Export data** | 1. Find template <br> 2. Click "Generate" <br> 3. Download |
| **Remove template** | 1. Uningest <br> 2. Wait for UNPROCESSED <br> 3. Delete |

---

**Document End**

*For technical documentation, see: WKF-TECHNICAL-ARCHITECTURE.md and WKF-INGESTION-COMPLETE-FIX-FINAL.md*

*Last updated: February 14, 2026*
