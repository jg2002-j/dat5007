describe("Planner API E2E - System Testing Suite", () => {
    const VALID_UUID = "550e8400-e29b-41d4-a716-446655440001";
    const TEST_DATE = "2026-05-25";

    before(() => {
        // Phase: Test Environment Setup (STLC Phase 4)
        cy.waitForPlannerApi();
    });

    // =========================================================================
    // TECHNIQUE: POSITIVE EQUIVALENCE PARTITIONING (Happy Path)
    // =========================================================================
    it("should successfully save and then retrieve a full day plan", () => {
        const payload = {
            date: TEST_DATE,
            mealUuids: {
                BREAKFAST: [VALID_UUID],
                LUNCH: [VALID_UUID]
            }
        };

        // Action: Save Plan
        cy.apiPost("/planner/save/day", payload).then((res) => {
            expect(res.status).to.eq(200);
        });

        // Action: Retrieve and Verify State (Persistence Check)
        cy.apiGet(`/planner/view/day/${TEST_DATE}`).then((res) => {
            expect(res.status).to.eq(200);
            expect(res.body.date).to.eq(TEST_DATE);
            expect(res.body.meals.BREAKFAST[0].name).to.eq("E2E Pasta");
        });
    });

    // =========================================================================
    // TECHNIQUE: NEGATIVE EQUIVALENCE PARTITIONING (Invalid Inputs)
    // =========================================================================
    it("should return 400 for invalid date format (yyyy-dd-mm)", () => {
        cy.apiGet("/planner/view/day/2026-25-05").then((res) => {
            expect(res.status).to.eq(400);
        });
    });

    it("should return 400 when a recipe UUID does not exist", () => {
        const payload = {
            date: "2026-01-01",
            mealUuids: {OTHER: ["non-existent-uuid"]}
        };
        cy.apiPost("/planner/save/day", payload).then((res) => {
            // After the service fix, this should be 400, not 500
            expect(res.status).to.eq(400);
        });
    });

    // =========================================================================
    // TECHNIQUE: BOUNDARY VALUE ANALYSIS (BVA)
    // =========================================================================
    it("should handle a plan with zero meals (Empty Boundary)", () => {
        const payload = {date: "2026-12-31", mealUuids: {}};
        cy.apiPost("/planner/save/day", payload).then((res) => {
            expect(res.status).to.eq(200);
            expect(res.body.meals).to.be.empty;
        });
    });

    // =========================================================================
    // TECHNIQUE: STATE TRANSITION (Idempotency)
    // =========================================================================
    it("should overwrite an existing plan for the same date", () => {
        const date = "2026-06-01";
        const firstPlan = {date: date, mealUuids: {BREAKFAST: [VALID_UUID]}};
        const secondPlan = {date: date, mealUuids: {DINNER: [VALID_UUID]}};

        cy.apiPost("/planner/save/day", firstPlan);
        cy.apiPost("/planner/save/day", secondPlan).then((res) => {
            expect(res.status).to.eq(200);
        });

        cy.apiGet(`/planner/view/day/${date}`).then((res) => {
            // State check: Should have DINNER, but BREAKFAST should be gone
            expect(res.body.meals).to.have.property("DINNER");
            expect(res.body.meals).to.not.have.property("BREAKFAST");
        });
    });
});