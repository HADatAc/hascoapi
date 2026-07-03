# WKF vs INACSL: Gap Analysis & Proposed Educational Properties

**Date:** July 3, 2026  
**Purpose:** Identify minimal WKF extensions needed to support INACSL simulation design standards  
**Focus:** Learning/Educational components

---

## 1. CONTRAST: WKF Template vs INACSL Guidelines

### Current WKF Capabilities ✅

| WKF Feature | INACSL Coverage | Assessment |
|-------------|-----------------|------------|
| **Process scenario (rdfs:comment)** | Criterion 5: Clinical case/backstory | ✅ **GOOD** - Supports detailed scenario context |
| **Task hierarchy (hasSupertask/hasSubtask)** | Criterion 5: Structured activities | ✅ **GOOD** - CTT task decomposition |
| **Temporal dependencies** | Criterion 5: Clinical progression | ✅ **GOOD** - Workflow sequencing |
| **RequiredInstruments** | Criterion 6: Physical fidelity | ✅ **GOOD** - Equipment/resources |
| **Top task reference** | Criterion 4: Structured activities | ✅ **GOOD** - Clear starting point |
| **Task types (CTT)** | Criterion 5: Task categorization | ✅ **GOOD** - User/System/Interaction tasks |
| **Version control** | Criterion 11: Iterative refinement | ✅ **ADEQUATE** - hasVersion tracking |
| **Status tracking** | Criterion 1: Review workflow | ✅ **ADEQUATE** - Draft/Review/Published |

### Critical INACSL Gaps ❌

| INACSL Criterion | Requirement | WKF Gap | Impact |
|------------------|-------------|---------|--------|
| **Criterion 3: Measurable Objectives** | Specific learning outcomes with observable behaviors | ❌ **No learning objectives field** | **CRITICAL** - Can't document educational intent |
| **Criterion 5: Critical Actions** | Performance measures aligned with objectives | ❌ **No performance criteria** | **CRITICAL** - Can't evaluate learner success |
| **Criterion 2: Needs Assessment** | Gap analysis, foundational evidence | ❌ **No needs documentation** | **HIGH** - Missing pedagogical rationale |
| **Criterion 9: Debriefing** | Structured reflection plan | ❌ **No debriefing guidance** | **HIGH** - Missing reflection framework |
| **Criterion 8: Prebriefing** | Preparation & psychological safety | ❌ **No prebriefing plan** | **MEDIUM** - Missing learner preparation |
| **Criterion 7: Facilitation** | Educator guidance, cueing plan | ⚠️ **Partial** - Tasks exist but no educator cues | **MEDIUM** - Limited facilitation support |
| **Criterion 10: Evaluation** | Learner & simulation assessment | ❌ **No evaluation framework** | **CRITICAL** - Can't measure effectiveness |
| **Criterion 6: Fidelity** | Physical/conceptual/psychological realism | ⚠️ **Partial** - Equipment yes, fidelity details no | **MEDIUM** - Limited realism specification |

---

## 2. MINIMAL CORE PROPERTIES NEEDED

### Educational Focus Analysis

**INACSL Core Educational Requirements:**
1. **Learning Objectives** (What learners should achieve) - **CRITICAL**
2. **Performance Criteria** (How to measure achievement) - **CRITICAL**  
3. **Debriefing Plan** (How to facilitate reflection) - **HIGH**
4. **Target Audience** (Who this is designed for) - **HIGH**
5. **Facilitation Guidance** (How educators guide learners) - **MEDIUM**

**User Constraint:** 2-3 properties maximum

**Recommended Priority:**

### ⭐ HIGHEST IMPACT: 3 Properties

These 3 properties address 5 of 11 INACSL criteria and cover the complete educational cycle:

1. **`vstoi:hasLearningObjectives`** → Criterion 3 (Measurable Objectives)
2. **`vstoi:hasCriticalActions`** → Criterion 5 (Performance Measures) + Criterion 10 (Evaluation)
3. **`vstoi:hasDebriefingFocus`** → Criterion 9 (Debriefing Plan)

---

## 3. PROPOSED PROPERTIES

### Property 1: `vstoi:hasLearningObjectives` ⭐⭐⭐

**Purpose:** Document specific, measurable learning outcomes for educational simulations

**INACSL Alignment:**
- ✅ **Criterion 3:** Measurable Objectives
- ✅ **Criterion 2:** Needs Assessment (objectives derived from identified gaps)
- ✅ **Criterion 10:** Evaluation (objectives drive assessment)

**Rationale:**
- **CRITICAL GAP:** WKF currently has NO way to specify what learners should achieve
- **Educational Core:** Objectives are foundational to ALL educational design
- **INACSL Requirement:** "Construct measurable objectives... determine which objectives to disclose pre-experience"

**Data Type:** String (structured list, semicolon-separated or JSON)

**Format Options:**

**Option A: Semicolon-Separated (Simple)**
```
Assess patient using Braden Scale (score documented); 
Identify ≥2 caregiver learning needs through interview; 
Develop evidence-based care plan with ≥1 community resource
```

**Option B: Structured JSON (Rich)**
```json
[
  {
    "domain": "cognitive",
    "level": "apply",
    "objective": "Assess patient using Braden Scale",
    "measurable_criteria": "Braden score documented correctly",
    "disclosed": true
  },
  {
    "domain": "affective", 
    "level": "respond",
    "objective": "Use therapeutic communication with challenging caregivers",
    "measurable_criteria": "Demonstrates ≥3 therapeutic techniques",
    "disclosed": true
  },
  {
    "domain": "psychomotor",
    "level": "perform",
    "objective": "Complete wound assessment using standardized terminology",
    "measurable_criteria": "All wound characteristics documented per protocol",
    "disclosed": false
  }
]
```

**Recommended:** Start with **Option A** (simple), allow **Option B** (rich) as optional enhancement

**Example Values:**
```
# Simple format
Demonstrate systematic patient assessment; 
Develop prioritized care plan; 
Communicate effectively with family caregivers

# With Bloom's taxonomy
[Apply] Conduct Braden Scale assessment (score documented); 
[Analyze] Identify ≥3 caregiver knowledge gaps; 
[Create] Develop evidence-based care plan with rationale
```

---

### Property 2: `vstoi:hasCriticalActions` ⭐⭐⭐

**Purpose:** Define essential performance criteria that learners must demonstrate for successful completion

**INACSL Alignment:**
- ✅ **Criterion 5:** Scenario Design - "Identify critical actions/performance measures"
- ✅ **Criterion 10:** Evaluation - Evidence-based assessment measures
- ✅ **Criterion 3:** Measurable Objectives - Observable behaviors

**Rationale:**
- **CRITICAL GAP:** No way to specify WHAT constitutes successful performance
- **Assessment Foundation:** Critical for evaluating learner competence
- **INACSL Requirement:** "Identify critical actions... evidence-based measures validated by content experts"
- **Safety Impact:** In healthcare, critical actions can be life-or-death decisions

**Data Type:** String (structured checklist, semicolon-separated or JSON)

**Format Options:**

**Option A: Simple Checklist (Semicolon-Separated)**
```
Introduce self to patient (not just caregivers); 
Obtain consent before touching patient; 
Complete Braden Scale assessment; 
Identify ≥2 incorrect caregiver practices; 
Use therapeutic communication (not confrontational); 
Identify ≥1 community resource
```

**Option B: Structured JSON (With Priority)**
```json
[
  {
    "action": "Introduce self to patient (not just caregivers)",
    "priority": "critical",
    "timing": "0-2 min",
    "category": "communication"
  },
  {
    "action": "Complete Braden Scale assessment (score documented)",
    "priority": "critical",
    "timing": "5-12 min",
    "category": "assessment"
  },
  {
    "action": "Identify misinformation tactfully",
    "priority": "expected",
    "timing": "12-18 min",
    "category": "education"
  }
]
```

**Recommended:** Start with **Option A** (simple), support **Option B** (advanced) for complex scenarios

**Example Values:**
```
# Basic format
Assess airway patency; 
Administer oxygen per protocol; 
Monitor vital signs every 5 min; 
Document interventions

# With categories
[MUST] Introduce self to patient; 
[MUST] Complete Braden assessment; 
[EXPECTED] Identify caregiver knowledge gaps; 
[ADVANCED] Navigate family conflict diplomatically
```

---

### Property 3: `vstoi:hasDebriefingFocus` ⭐⭐

**Purpose:** Guide structured post-simulation reflection and learning consolidation

**INACSL Alignment:**
- ✅ **Criterion 9:** Debriefing - "Purposeful reflection... facilitate sense-making"
- ✅ **Criterion 3:** Objectives - Link debriefing to learning outcomes
- ✅ **Criterion 10:** Evaluation - Formative assessment through reflection

**Rationale:**
- **HIGH IMPACT:** Debriefing is where learning deepens
- **INACSL Emphasis:** "Debriefing is an active process... essential for meaningful learning"
- **Educational Gap:** WKF has scenario but no reflection guidance
- **Facilitator Support:** Helps educators structure productive discussion

**Data Type:** String (structured topics, semicolon-separated or JSON)

**Format Options:**

**Option A: Simple Topic List (Semicolon-Separated)**
```
Clinical reasoning: How did you prioritize patient needs?; 
Communication: What strategies worked with challenging caregivers?; 
Professional boundaries: How did you balance family autonomy with evidence-based practice?; 
Resource utilization: What community resources did you identify?
```

**Option B: Structured JSON (With Framework)**
```json
{
  "framework": "DASH",
  "debriefing_elements": [
    {
      "phase": "reactions",
      "focus": "What surprised you about the family dynamics?",
      "timing": "2-3 min"
    },
    {
      "phase": "analysis", 
      "focus": "Why did you choose to address the wound care misinformation directly vs. indirectly?",
      "timing": "8-10 min"
    },
    {
      "phase": "summary",
      "focus": "What will you do differently in future home visits?",
      "timing": "3-5 min"
    }
  ]
}
```

**Recommended:** Start with **Option A** (topic list), allow **Option B** (framework-based) as enhancement

**Example Values:**
```
# Topic-based format
Assessment skills: completeness and accuracy; 
Communication techniques: therapeutic vs confrontational; 
Clinical reasoning: priority-setting rationale; 
Cultural competence: respect for family values

# Question-based format
What went well in your assessment?; 
What would you do differently?; 
How did you balance caregiver misinformation with therapeutic relationship?; 
What resources did you identify and why?
```

---

## 4. PROPERTY PLACEMENT IN WKF TEMPLATE

### Where These Properties Fit

All 3 properties should be added to the **Processes** sheet because:
- ✅ Processes define concrete scenarios (not abstract ProcessStems)
- ✅ Learning objectives are scenario-specific
- ✅ Critical actions depend on specific clinical case
- ✅ Debriefing focuses on specific scenario challenges
- ✅ Maintains WKF "self-contained template" philosophy

### Updated Processes Sheet Structure

**Current Columns (15):**
```
A: hasURI
B: rdf:type
C: hasco:hascoType
D: rdfs:label
E: rdfs:comment (scenario description)
F: vstoi:hasStatus
G: vstoi:hasLanguage
H: vstoi:hasVersion
I: prov:wasDerivedFrom (→ ProcessStem)
J: vstoi:hasReviewNote
K: vstoi:hasSIRManagerEmail
L: vstoi:hasEditorEmail
M: vstoi:hasTopTask (→ root Task)
N: hasco:hasImage
O: hasco:hasWebDocument
```

**Proposed Addition (3 new columns):**
```
P: vstoi:hasLearningObjectives       [NEW - EDUCATIONAL]
Q: vstoi:hasCriticalActions           [NEW - EDUCATIONAL]
R: vstoi:hasDebriefingFocus           [NEW - EDUCATIONAL]
```

**Final Structure: 18 columns (15 existing + 3 new)**

### Column Details

| Column | Property | Type | Required | Description |
|--------|----------|------|----------|-------------|
| **P** | `vstoi:hasLearningObjectives` | String | **Conditional*** | Measurable learning outcomes (semicolon-separated) |
| **Q** | `vstoi:hasCriticalActions` | String | **Conditional*** | Essential performance criteria (semicolon-separated) |
| **R** | `vstoi:hasDebriefingFocus` | String | **Conditional*** | Key reflection topics/questions (semicolon-separated) |

**Required = Conditional:**
- **REQUIRED** for educational/training workflows (vstoi:hascoType includes "Training", "Simulation", "Educational")
- **OPTIONAL** for operational/clinical workflows (pure process documentation)
- **RECOMMENDED** for all Processes that will be used for assessment or learning

---

## 5. EXAMPLE IMPLEMENTATION

### Scenario: Home Care Simulation (from Guião)

**Process Entry WITH New Properties:**

| Column | Property | Value |
|--------|----------|-------|
| A | hasURI | `http://pmsr.net/ont/pmsr#/WKF_HOME_CARE_WOUND_0001/PROC/0001` |
| B | rdf:type | `vstoi:Process` |
| C | hasco:hascoType | `vstoi:ClinicalSimulation` |
| D | rdfs:label | `Home Visit: Dependent Elder with Pressure Injuries` |
| E | rdfs:comment | `Sr. José, 76yo widower, bedridden with high PU risk (Braden 11), aphasia 6mo post-CVA. Multiple pressure injuries. First home visit requested by family. Caregivers well-educated but misinformed about wound care.` |
| F | vstoi:hasStatus | `vstoi:Published` |
| G | vstoi:hasLanguage | `pt` |
| H | vstoi:hasVersion | `1.0` |
| I | prov:wasDerivedFrom | `http://pmsr.net/ont/pmsr#/WKF_HOME_CARE_WOUND_0001/PST/0001` |
| J | vstoi:hasReviewNote | `Validated by wound care CNS 2026-06-15` |
| K | vstoi:hasSIRManagerEmail | `joao.instructor@pmsr.net` |
| L | vstoi:hasEditorEmail | `nursing.simulation@pmsr.net` |
| M | vstoi:hasTopTask | `http://pmsr.net/ont/pmsr#/WKF_HOME_CARE_WOUND_0001/TSK/0001` |
| N | hasco:hasImage | `http://pmsr.net/scenarios/images/home-care-wound.jpg` |
| O | hasco:hasWebDocument | `http://pmsr.net/scenarios/docs/home-care-wound-guide.pdf` |
| **P** | **vstoi:hasLearningObjectives** | **`Conduct systematic home assessment using Braden Scale (score documented); Identify ≥2 caregiver learning needs through observation/interview; Develop evidence-based care plan with ≥1 community resource referral; Use therapeutic communication techniques with challenging caregivers`** |
| **Q** | **vstoi:hasCriticalActions** | **`[MUST] Introduce self to patient first (not just caregivers); [MUST] Obtain consent before assessment; [MUST] Complete Braden Scale (score 11); [MUST] Identify ≥2 incorrect practices tactfully; [EXPECTED] Identify community resource (pressure-relief mattress OR dietitian); [EXPECTED] Use teach-back method for education`** |
| **R** | **vstoi:hasDebriefingFocus** | **`Clinical reasoning: How did you prioritize multiple patient/caregiver needs?; Communication: What strategies worked with the defensive caregiver? What didn't work?; Professional boundaries: How did you balance evidence-based practice with family autonomy?; Resource identification: What community resources did you consider and why?`** |

---

## 6. IMPLEMENTATION GUIDANCE

### For WKF Template Creators

**When to Use These Properties:**

| Workflow Type | Learning Objectives | Critical Actions | Debriefing Focus |
|---------------|---------------------|------------------|------------------|
| **Educational Simulation** | ✅ REQUIRED | ✅ REQUIRED | ✅ REQUIRED |
| **Training Protocol** | ✅ REQUIRED | ✅ REQUIRED | ✅ RECOMMENDED |
| **Clinical Procedure (teaching)** | ✅ RECOMMENDED | ✅ RECOMMENDED | ⚪ OPTIONAL |
| **Operational Workflow** | ⚪ OPTIONAL | ⚪ OPTIONAL | ⚪ OPTIONAL |
| **Research Protocol** | ⚪ OPTIONAL | ⚪ OPTIONAL | ⚪ OPTIONAL |

**Content Development Tips:**

**Learning Objectives:**
- Start with Bloom's taxonomy verbs (Apply, Analyze, Evaluate, Create)
- Make observable and measurable ("demonstrate", "identify", "develop")
- Align with curriculum competencies
- 3-5 objectives per scenario (max 7)

**Critical Actions:**
- Identify MUST-DO actions (safety, legal, ethical)
- Separate critical (must demonstrate) from expected (should demonstrate) from advanced (nice to have)
- Keep list to 5-8 critical actions (manageable for assessment)
- Validate with content experts

**Debriefing Focus:**
- Frame as open-ended questions (not yes/no)
- Link to learning objectives
- Cover cognitive (what), affective (how felt), psychomotor (what did)
- Include "What will you do differently next time?"

### For Educators Using WKF

**Pre-Simulation:**
- Review `vstoi:hasLearningObjectives` → Share with learners (or keep some hidden)
- Review `vstoi:hasCriticalActions` → Print as assessment checklist
- Review `vstoi:hasDebriefingFocus` → Prepare debriefing questions

**During Simulation:**
- Use `vstoi:hasCriticalActions` for real-time performance tracking

**Post-Simulation:**
- Use `vstoi:hasDebriefingFocus` to structure reflection
- Refer back to `vstoi:hasLearningObjectives` - were they met?

### For System Developers

**Backend Implementation:**

```java
// Add to Process entity (Process.java)
@PropertyField(uri="vstoi:hasLearningObjectives")
private String hasLearningObjectives;

@PropertyField(uri="vstoi:hasCriticalActions")
private String hasCriticalActions;

@PropertyField(uri="vstoi:hasDebriefingFocus")
private String hasDebriefingFocus;

// Add getters/setters + SPARQL parsing
```

**Validation Rules:**

```java
// In AnnotateWKF.java validation
if (process.getHascoType().contains("Simulation") || 
    process.getHascoType().contains("Training")) {
    
    if (process.getHasLearningObjectives() == null) {
        logger.warn("Educational process missing learning objectives");
    }
    if (process.getHasCriticalActions() == null) {
        logger.warn("Educational process missing critical actions");
    }
}
```

**UI Display:**

- Create dedicated "Educational Properties" section in process view
- Parse semicolon-separated values into bullet lists
- Support both simple (semicolon) and structured (JSON) formats
- Provide templates/examples for content creators

---

## 7. IMPACT ASSESSMENT

### INACSL Compliance Improvement

**Before (Current WKF):**
- 0 of 11 INACSL criteria explicitly supported for educational design
- ⚠️ Partial support: Criterion 5 (scenario structure via Tasks)

**After (WKF + 3 Properties):**
- ✅ **5 of 11 INACSL criteria** explicitly supported:
  - Criterion 3: Measurable Objectives → `vstoi:hasLearningObjectives`
  - Criterion 5: Critical Actions → `vstoi:hasCriticalActions`
  - Criterion 9: Debriefing → `vstoi:hasDebriefingFocus`
  - Criterion 10: Evaluation → `vstoi:hasCriticalActions` (performance checklist)
  - Criterion 2: Needs Assessment → `vstoi:hasLearningObjectives` (derived from needs)

**Remaining Gaps (would require additional properties):**
- Criterion 1: Expert design documentation
- Criterion 4: Theoretical framework
- Criterion 6: Fidelity details (physical/conceptual/psychological)
- Criterion 7: Facilitation/cueing plan
- Criterion 8: Prebriefing plan
- Criterion 11: Pilot testing documentation

**Coverage:** 45% of INACSL criteria with just 3 properties ✅

### Educational Value Added

**For Learners:**
- ✅ Clear expectations (learning objectives)
- ✅ Transparent assessment (critical actions)
- ✅ Structured reflection (debriefing focus)

**For Educators:**
- ✅ Standardized teaching guide
- ✅ Objective assessment criteria
- ✅ Debriefing framework

**For Programs:**
- ✅ Curriculum alignment (objectives map to competencies)
- ✅ Quality assurance (consistent assessment)
- ✅ Continuous improvement (evaluate effectiveness)

---

## 8. ALTERNATIVE MINIMAL OPTIONS

If only **2 properties** are feasible:

### Option A: Assessment-Focused
1. **`vstoi:hasLearningObjectives`** (what to learn)
2. **`vstoi:hasCriticalActions`** (how to assess)

**Rationale:** Covers objective-setting and evaluation (most critical educational cycle)

### Option B: Learning-Focused  
1. **`vstoi:hasLearningObjectives`** (what to learn)
2. **`vstoi:hasDebriefingFocus`** (how to reflect)

**Rationale:** Covers before-and-after learning experience

### Recommendation
Implement all **3 properties** - they work as an integrated educational framework:
- **Before:** Learning Objectives (expectation-setting)
- **During:** Critical Actions (performance guidance)
- **After:** Debriefing Focus (reflection/consolidation)

---

## 9. SUMMARY

### Proposed Minimal WKF Extensions

| # | Property | Sheet | INACSL Coverage | Educational Impact |
|---|----------|-------|-----------------|-------------------|
| 1 | `vstoi:hasLearningObjectives` | Processes | Criteria 2, 3, 10 | ⭐⭐⭐ CRITICAL |
| 2 | `vstoi:hasCriticalActions` | Processes | Criteria 5, 10 | ⭐⭐⭐ CRITICAL |
| 3 | `vstoi:hasDebriefingFocus` | Processes | Criteria 3, 9, 10 | ⭐⭐ HIGH |

### Implementation Simplicity
- ✅ Only 3 new columns in Processes sheet
- ✅ Simple string data type (semicolon-separated)
- ✅ Optional rich format (JSON) for advanced users
- ✅ Backward compatible (optional fields)
- ✅ No changes to other sheets (ProcessStems, Tasks, RequiredInstruments)

### Educational Community Alignment
- ✅ Addresses **learning/educational focus** (per user requirement)
- ✅ Supports Bloom's taxonomy and evidence-based education
- ✅ Aligns with international simulation standards (INACSL)
- ✅ Enables objective assessment and program evaluation
- ✅ Minimal burden on template creators (2-3 fields)

**Final Recommendation:** Implement all 3 properties as **conditional requirements** for educational workflows, **optional** for operational workflows.
