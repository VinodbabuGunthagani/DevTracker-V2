
document.addEventListener("DOMContentLoaded", () => {
    const choiceBox = document.getElementById("choiceBox");
    const manualForm = document.getElementById("manualForm");
    const aiForm = document.getElementById("aiForm");
    const roadmapForm = document.getElementById("roadmapForm");
    const message = document.getElementById("manualMessage");
    const createButton = document.getElementById("createRoadmapBtn");

    // Show the selected section.
    function showSection(section) {
        choiceBox.style.display = section === "choices" ? "grid" : "none";
        manualForm.style.display = section === "manual" ? "flex" : "none";
        aiForm.style.display = section === "ai" ? "flex" : "none";
    }

    // These functions are called by onclick attributes in the HTML.
    window.showManualForm = () => showSection("manual");
    window.showAiForm = () => showSection("ai");
    window.goBackChoice = () => showSection("choices");

    // Load the logged-in user's dashboard identity.
    async function loadUser() {
        try {
            const response = await fetch("/api/dashboard", {
                method: "GET",
                credentials: "include"
            });

            if (response.status === 401) {
                window.location.href = "/login.html";
                return;
            }

            if (!response.ok) return;

            const data = await response.json();

            document.getElementById("sidebarUserName").textContent =
                data.userName || "";

            document.getElementById("initials").textContent =
                data.initials || "";
        } catch (error) {
            console.error("Could not load user details:", error);
        }
    }

    loadUser();

    // Display form messages.
    function showMessage(text, isError) {
        message.textContent = text;
        message.className = isError
            ? "message error"
            : "message success";
    }

    // Create a manual roadmap.
    roadmapForm.addEventListener("submit", async event => {
        event.preventDefault();

        message.textContent = "";

        const title = document.getElementById("roadmapTitle").value.trim();
        const description =
            document.getElementById("roadmapDescription").value.trim();
        const startDate = document.getElementById("startDate").value;
        const targetDate = document.getElementById("targetDate").value;

        if (!title) {
            showMessage("Please enter a roadmap name.", true);
            return;
        }

        if (startDate && targetDate && targetDate < startDate) {
            showMessage(
                "Target date cannot be before the start date.",
                true
            );
            return;
        }

        createButton.disabled = true;
        createButton.textContent = "Creating...";

        try {
            const response = await fetch("/api/roadmaps", {
                method: "POST",
                credentials: "include",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    title,
                    description,
                    startDate,
                    targetDate
                })
            });

            const result = await response.json().catch(() => ({}));

            if (response.status === 401) {
                window.location.href = "/login.html";
                return;
            }

            if (!response.ok) {
                throw new Error(
                    result.message || "Could not create roadmap."
                );
            }

            showMessage(
                "Roadmap created successfully. Opening dashboard...",
                false
            );

            window.location.href = "/dashboard.html";
        } catch (error) {
            showMessage(
                error.message || "Something went wrong.",
                true
            );

            createButton.disabled = false;
            createButton.innerHTML =
                '<i class="ti ti-plus"></i> Create Roadmap';
        }
    });

    // AI generation is not connected to the V2 API yet.
    window.setUnit = unit => {
        const timeInput = document.getElementById("dailyTimeInput");
        const minButton = document.getElementById("unitMinBtn");
        const hourButton = document.getElementById("unitHourBtn");

        if (!timeInput || !minButton || !hourButton) return;

        minButton.style.background =
            unit === "min" ? "var(--accent-fill)" : "";
        minButton.style.color =
            unit === "min" ? "#2b1a05" : "";

        hourButton.style.background =
            unit === "hour" ? "var(--accent-fill)" : "";
        hourButton.style.color =
            unit === "hour" ? "#2b1a05" : "";
    };

    window.handleAiSubmit = event => {
        event.preventDefault();

        const aiMessage = document.getElementById("aiMessage");
        aiMessage.textContent =
            "AI roadmap generation is not connected to the V2 backend yet.";

        return false;
    };
});
