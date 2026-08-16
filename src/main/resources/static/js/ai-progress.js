const progressFill = document.getElementById("progressFill");
const progressText = document.getElementById("progressText");
const completedText = document.getElementById("completedText");

const currentSubject = document.getElementById("currentSubject");
const currentTopic = document.getElementById("currentTopic");

const taskList = document.getElementById("taskList");

const statusText = document.getElementById("statusText");

let timer = null;

function loadProgress() {

	fetch("aiJobProgress?jobId=" + jobId)
		.then(response => response.json())
		.then(data => {
			console.log(data);
			let percent = 0;

			if (data.total > 0) {
				percent = Math.round((data.completed * 100) / data.total);
			}

			progressFill.style.width = percent + "%";

			progressText.innerText = percent + "%";

			completedText.innerText =
				data.completed + " / " + data.total + " Completed";

			taskList.innerHTML = "";

			let runningTask = null;

			data.tasks.forEach(task => {

				const row = document.createElement("div");
				row.className = "task-row";

				let icon = "○";

				switch (task.status) {

					case "COMPLETED":
						icon = "✅";
						break;

					case "RUNNING":
						icon = "⏳";
						runningTask = task;
						break;

					case "FAILED":
						icon = "❌";
						break;

					default:
						icon = "○";
				}

				row.innerHTML = `
                    <span class="task-icon">${icon}</span>

                    <div class="task-info">

                        <div class="task-name">${task.subtopicName}</div>

                        <div class="task-path">
                            ${task.subjectName} → ${task.topicName}
                        </div>

                    </div>
                `;

				taskList.appendChild(row);

			});

			if (runningTask != null) {

				currentSubject.innerText = runningTask.subjectName;
				currentTopic.innerText = runningTask.topicName;

				statusText.innerText =
					"Generating: " + runningTask.subtopicName;

			} else {

				currentSubject.innerText = "-";
				currentTopic.innerText = "-";

				if (data.status === "COMPLETED") {

					statusText.innerText =
						"🎉 AI Learning Generated Successfully";

				} else if (data.status === "FAILED") {

					statusText.innerText =
						"❌ Generation Failed";

				} else {

					statusText.innerText =
						"Waiting for AI worker...";
				}

			}

			if (data.status === "COMPLETED" ||
				data.status === "FAILED") {

				clearInterval(timer);

			}

		})
		.catch(error => {

			console.error(error);

			statusText.innerText =
				"Unable to fetch progress.";

		});

}

loadProgress();

timer = setInterval(loadProgress, 2000);