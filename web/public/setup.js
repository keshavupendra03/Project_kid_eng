const form = document.getElementById("setup");
const message = document.getElementById("message");
const errors = {
  forbidden: "Wrong setup key.",
  already_set_up: "The owner account already exists.",
  bad_username: "Username: 3 or more letters or numbers, no spaces.",
  weak_password: "Password is too short.",
};

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  const response = await fetch("/api/setup", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(Object.fromEntries(new FormData(form))),
  });
  const body = await response.json();
  if (response.ok) location.href = "/";
  else message.textContent = errors[body.error] ?? "Something went wrong.";
});
