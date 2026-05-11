# WRIT1 Report Quick Reference Guide

**Purpose**: One-page guide to navigate the WRIT1 academic outline and submit the final report.

---

## 📋 What you have

✅ **WRIT1-PLAN.md** — A complete 395-line academic outline with:
- All 6 required report sections
- Expected word counts per section
- Detailed content guidance for each section
- 32 complete test case specifications (unit, integration, system)
- Coverage metrics, defect analysis, and conclusion framework

This is your blueprint. It's ready to be filled in with prose, evidence, and visuals.

---

## 🚀 How to use this outline

### Step 1: Copy the outline as your report skeleton

```bash
cp docs/testing/WRIT1-PLAN.md docs/testing/FINAL-WRIT1-REPORT.md
```

Open `FINAL-WRIT1-REPORT.md` in your word processor (e.g., NotePad, Google Docs, Word).

### Step 2: Expand each section into prose

Replace the section headings and bullet points with:
- **Full paragraphs** (1–2 sentences per point)
- **Transitions** between ideas
- **Evidence** (screenshots, code examples, tables from existing docs)

Target word counts per section:
- **Introduction**: 400–500 words
- **Case Study**: 700–900 words
- **Testing Plan**: 900–1,100 words
- **Test Case Design**: 1,500–1,800 words
- **Test Execution**: 1,500–1,700 words
- **Conclusion**: 400–500 words
- **References**: 100–200 words

**Total target: 6,000 words** (excluding references and appendices)

### Step 3: Add evidence

Reference or attach:
- Screenshots of API endpoints (Postman or browser)
- Database schema diagram (from Flyway migrations)
- JaCoCo coverage report excerpt (HTML report screenshot)
- Control Flow Graph diagram (from `docs/testing/CFG-ANALYSIS.md`)
- RTM table (from `docs/testing/RTM.md`)
- Defect log summary (from `docs/defect-log/defect-log.csv`)

### Step 4: Format according to assessment brief

- **Font**: Arial 12pt
- **Line spacing**: 1.5
- **Margins**: Standard (1 inch / 2.54 cm)
- **Page numbers**: Bottom right
- **Table of contents**: After title page
- **References**: Alphabetical, on separate page(s)

### Step 5: Self-check against rubric

Rate yourself honestly against the 7 marking criteria:

| Criterion | Target % | Self-rated | Evidence |
|-----------|----------|-----------|----------|
| Introduction | 5% | ? | Clear STLC overview? |
| Case Study | 10% | ? | Good system description + screenshots? |
| Testing Plan | 15% | ? | Scope, techniques, tools well explained? |
| Test Design | 30% | ? | Test tables detailed + justified? |
| Test Execution | 30% | ? | Results, defects, coverage shown? |
| Conclusion | 5% | ? | Critical evaluation included? |
| Writing & Refs | 5% | ? | Proper formatting + citations? |

Aim for **40/100 minimum** (50% of 80 marks for postgraduate pass).

### Step 6: Submit

Follow submission instructions from your module coordinator.

---

## 🎯 Key tips for high marks

### For Test Design (30% weighting — biggest opportunity)

✅ **Do**:
- Explain WHY each technique fits its test case
- Show the CFG or decision tree for complex methods
- Link each test ID to the requirement it covers
- Describe input partitions in detail (not just "valid/invalid")

❌ **Don't**:
- Just list test cases without justification
- Forget to explain what EP or BVA means in your context
- Leave blank rows in your test case tables

### For Test Execution (30% weighting — critical to pass)

✅ **Do**:
- Show actual pass/fail results (even if all pass)
- Include coverage percentages with interpretation
- Describe the 5 defects and their severity
- Explain what the coverage metrics prove AND what they don't

❌ **Don't**:
- Skip screenshots of the coverage report
- Assume readers know what "92% statement coverage" means
- Ignore defects or say none were found

### For Introduction (5% weighting)

✅ **Do**:
- Open with the testing philosophy (defects ≠ fixes)
- Define STLC phases clearly
- Explain observability and controllability
- State the report scope upfront

❌ **Don't**:
- Write a generic "testing is important" intro
- Spend too many words here (you need space for test design/execution)

---

## 📁 Existing resources to cite

These are already in your repository; reference them in your report:

| Document | Purpose | Citation |
|----------|---------|----------|
| `docs/testing/RTM.md` | Requirement Traceability Matrix | Link in Appendix A |
| `docs/testing/CFG-ANALYSIS.md` | Control Flow Graph diagrams | Reference for white-box section |
| `docs/testing/STLC-SUMMARY.md` | STLC phase summary | Optional background reading |
| `docs/testing/TEST-EXECUTION.md` | How to run tests | Appendix or methodology |
| `docs/defect-log/defect-log.csv` | Defect tracking template | Appendix C or inline table |
| `README.md` | System overview | Cite in Case Study |
| `src/test/resources/notes.txt` | Module lecture notes | Primary source for theory |

---

## ✍️ Before you submit

### Final checklist

- [ ] Report is 6,000 words (±10%)
- [ ] All 6 sections present and complete
- [ ] Test case tables have inputs, outputs, techniques, and justification
- [ ] Coverage metrics explained, not just percentages
- [ ] Defect log tables included
- [ ] At least 3 screenshots or figures
- [ ] References formatted (Harvard, APA, or module standard)
- [ ] Spelling and grammar checked
- [ ] No placeholder text like "[TO BE FILLED IN]"
- [ ] Page numbers and section headings consistent
- [ ] Appendices linked from main text

### Proofread loop

1. Read the Introduction aloud; does it flow?
2. Check that every table is captioned and referenced in text.
3. Verify every figure/screenshot has a caption and reference.
4. Confirm each test case ID appears in the narrative at least once.
5. Scan for orphaned bullet points; convert to prose sentences.

---

## 🤔 FAQ

**Q: Should I run the tests myself or use the existing results?**  
A: **Run them yourself.** Doing so shows mastery and gives you actual evidence to cite.

**Q: The outline says 32 test cases but the repo has 33. What do I do?**  
A: Use all 33! The outline is a minimum; more test cases strengthen your report if justified.

**Q: My test case table is really long. Should I shorten it?**  
A: No. The test design section is 30% of marks. A long, well-justified table is evidence of depth.

**Q: Do I include the static testing (Phase 1) in my report?**  
A: Yes! RTM is part of Phase 1. Explain how requirements were analyzed before tests were written.

**Q: Can I use diagrams instead of tables?**  
A: Yes, but tables are more rigorous for academic reports. Use diagrams to support, not replace, tables.

**Q: What if my coverage is less than 85%?**  
A: Explain why (e.g., external API not 100% mocked) and discuss the risk. Honesty is better than claims you can't back up.

---

## 📞 Support resources

- **Module notes** (see `src/test/resources/notes.txt`): Reference STLC, testing techniques, cost of quality.
- **Existing test classes** (`src/test/java/...`): Use as concrete examples in your report.
- **JaCoCo report** (`target/jacoco-report/index.html`): Screenshot for coverage evidence.
- **Defect log** (`docs/defect-log/defect-log.csv`): Populate and include in appendices.

---

## 🎓 Final thought

This outline is designed to help you write a report that **demonstrates mastery of STLC theory and practice**. The structure matches the marking rubric; if you fill in each section thoroughly, you'll have a strong submission.

**Good luck!**

---

**Last Updated**: 2026-05-11  
**Status**: ✅ Ready to use  
**Next**: Open WRIT1-PLAN.md and start writing

