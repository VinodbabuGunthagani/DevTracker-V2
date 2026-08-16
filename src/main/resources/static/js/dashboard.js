document.addEventListener("DOMContentLoaded", loadDashboard);


async function loadDashboard() {

    try {

        const response = await fetch("/api/dashboard", {
            method: "GET",
            credentials: "include"
        });


        // User is not authenticated
        if (response.status === 401) {

            window.location.href = "/login.html";
            return;
        }


        if (!response.ok) {

            throw new Error(
                "Failed to load dashboard"
            );
        }


        const data = await response.json();


        // =========================
        // USER
        // =========================

        document.getElementById("initials").textContent =
            data.initials || "";

        document.getElementById("sidebarUserName").textContent =
            data.userName || "";

        const firstName =
            data.userName
                ? data.userName.trim().split(/\s+/)[0]
                : "";

        document.getElementById("welcomeName").textContent =
            firstName;


        // =========================
        // STATISTICS
        // =========================

        document.getElementById("totalRoadmaps").textContent =
            data.total ?? 0;

        document.getElementById("avgProgress").textContent =
            (data.avgProgress ?? 0) + "%";

        document.getElementById("completedRoadmaps").textContent =
            data.completed ?? 0;

        document.getElementById("aiCount").textContent =
            data.aiCount ?? 0;


        // =========================
        // ROADMAP COUNT
        // =========================

        document.getElementById("roadmapCount").textContent =
            `${data.total ?? 0} active`;


        // =========================
        // ROADMAP CARDS
        // =========================

        renderRoadmaps(data.roadmaps || []);


    } catch (error) {

        console.error(
            "Dashboard loading error:",
            error
        );

    }

}



function renderRoadmaps(roadmaps) {

    const grid =
        document.getElementById("roadmapGrid");

    grid.innerHTML = "";


    // No roadmaps
    if (roadmaps.length === 0) {

        const emptyMessage =
            document.createElement("p");

        emptyMessage.textContent =
            "No roadmaps yet. Create one to get started.";

        emptyMessage.style.color =
            "var(--faint)";

        emptyMessage.style.fontSize =
            "14px";

        emptyMessage.style.gridColumn =
            "1 / -1";

        grid.appendChild(emptyMessage);

    }


    // Roadmap cards
    roadmaps.forEach(roadmap => {

        const card =
            createRoadmapCard(roadmap);

        grid.appendChild(card);

    });


    // Add new roadmap card
    grid.appendChild(
        createNewRoadmapCard()
    );

}



function createRoadmapCard(roadmap) {

    const progress =
        roadmap.progress ?? 0;

    const isDone =
        progress >= 100;

    const fillClass =
        isDone
            ? "high"
            : progress >= 40
                ? "medium"
                : "low";


    const card =
        document.createElement("div");

    card.className = "card";


    // =========================
    // BADGES
    // =========================

    let badges = "";

    if (roadmap.ai) {

        badges += `
            <span class="badge badge-ai">
                <i class="ti ti-sparkles"
                   style="font-size:10px"></i>
                AI
            </span>
        `;

    }


    if (isDone) {

        badges += `
            <span class="badge badge-done">
                <i class="ti ti-check"
                   style="font-size:10px"></i>
                Completed
            </span>
        `;

    }


    // =========================
    // CARD
    // =========================

    card.innerHTML = `

        <div class="card-head">

            <div class="card-head-left">

                <div class="card-badges">
                    ${badges}
                </div>

                <div class="card-title">
                    ${escapeHtml(roadmap.title)}
                </div>

            </div>


            <button
                class="card-edit-btn"
                title="Edit"
                onclick="editRoadmap(${roadmap.id})">

                <i class="ti ti-pencil"></i>

            </button>

        </div>


        <p class="card-desc">
            ${escapeHtml(
                roadmap.description || ""
            )}
        </p>


        <div class="card-bottom">


            <!-- Progress -->

            <div>

                <div class="progress-row">

                    <span>
                        Progress
                    </span>

                    <strong>
                        ${progress}%
                    </strong>

                </div>


                <div class="progress-track">

                    <div
                        class="progress-fill ${fillClass}"
                        style="width:${progress}%">

                    </div>

                </div>

            </div>


            <!-- Dates -->

            <div class="date-row">

                <i class="ti ti-calendar"></i>

                ${formatDate(roadmap.startDate)}

                &ndash;

                ${formatDate(roadmap.targetDate)}

            </div>


            <!-- Actions -->

            <div class="card-actions">

                <a
                    class="btn btn-sm"
                    href="/openAIRoadmap?id=${roadmap.id}">

                    <i class="ti ti-map"></i>
                    Syllabus

                </a>


                <a
                    class="btn btn-sm"
                    href="/schedule.html?roadmapId=${roadmap.id}">

                    <i class="ti ti-calendar-event"></i>
                    Daily Task

                </a>


                <button
                    class="btn btn-sm btn-danger"
                    onclick="deleteRoadmap(${roadmap.id})">

                    <i class="ti ti-trash"></i>
                    Delete

                </button>

            </div>

        </div>

    `;


    return card;

}



function createNewRoadmapCard() {

    const card =
        document.createElement("a");

    card.className =
        "card-empty";

    card.href =
        "/create-roadmap.html";


    card.innerHTML = `

        <i class="ti ti-plus"></i>

        <p>
            Start a new roadmap
        </p>

    `;


    return card;

}



function editRoadmap(id) {

    // We will connect this to the V2
    // PUT/PATCH roadmap API later.

    console.log(
        "Edit roadmap:",
        id
    );

}



async function deleteRoadmap(id) {

    // V2 delete API will be connected
    // after roadmap CRUD migration.

    console.log(
        "Delete roadmap:",
        id
    );

}



function formatDate(date) {

    if (!date) {
        return "";
    }

    return date;

}



function escapeHtml(value) {

    if (value === null ||
        value === undefined) {

        return "";

    }

    const div =
        document.createElement("div");

    div.textContent =
        value;

    return div.innerHTML;

}