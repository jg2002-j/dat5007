describe("Planner API E2E", () => {
  before(() => {
    cy.waitForPlannerApi().then((response) => {
      expect([200, 400]).to.include(response.status);
    });
  });

  it("saves a day plan with empty meals", () => {
    cy.fixture("day-plan-empty.json").then((payload) => {
      cy.apiPost("/planner/save/day", payload).then((response) => {
        expect(response.status).to.eq(200);
        expect(response.body.date).to.eq(payload.date);
        expect(response.body.meals).to.deep.eq({});
      });
    });
  });

  it("saves and then retrieves a day plan", () => {
    cy.fixture("day-plan-empty-slot.json").then((payload) => {
      cy.apiPost("/planner/save/day", payload).then((saveResponse) => {
        expect(saveResponse.status).to.eq(200);
        expect(saveResponse.body.date).to.eq(payload.date);
      });

      cy.apiGet(`/planner/view/day/${payload.date}`).then((viewResponse) => {
        expect(viewResponse.status).to.eq(200);
        expect(viewResponse.body.date).to.eq(payload.date);
        expect(viewResponse.body.meals).to.have.property("BREAKFAST");
        expect(viewResponse.body.meals.BREAKFAST).to.be.an("array");
      });
    });
  });

  it("returns 500 when saving with null date", () => {
    cy.fixture("day-plan-null-date.json").then((payload) => {
      cy.apiPost("/planner/save/day", payload).then((response) => {
        expect(response.status).to.eq(500);
      });
    });
  });

  it("returns 400 for invalid date format in view endpoint", () => {
    cy.apiGet("/planner/view/day/27-04-2026").then((response) => {
      expect(response.status).to.eq(400);
      expect(response.body).to.have.property("message");
    });
  });

  it("returns 400 for partial date in view endpoint", () => {
    cy.apiGet("/planner/view/day/2026-04").then((response) => {
      expect(response.status).to.eq(400);
      expect(response.body).to.have.property("message");
    });
  });
});

