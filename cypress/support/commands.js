Cypress.Commands.add("apiGet", (path, options = {}) => {
  return cy.request({
	method: "GET",
	url: path,
	failOnStatusCode: false,
	...options,
  });
});

Cypress.Commands.add("apiPost", (path, body, options = {}) => {
  return cy.request({
	method: "POST",
	url: path,
	body,
	failOnStatusCode: false,
	...options,
  });
});

Cypress.Commands.add("waitForPlannerApi", () => {
  return cy.apiGet("/planner/view/day/2026-01-01", { timeout: 20000 });
});
