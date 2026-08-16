const loadingMessages = {

    default:[
        "checking resources...",
        "Loading your data...",
        "Organizing...",
        "Almost ready..."
    ],

    roadmap:[
        "Understanding your goal...",
        "Designing roadmap...",
        "Creating subjects...",
        "Building learning path...",
        "Almost ready..."
    ],

    schedule:[
        "Calculating study time...",
        "Balancing workload...",
        "Creating daily tasks...",
        "Optimizing your schedule..."
    ],

    learning:[
        "Generating lesson...",
        "Finding examples...",
        "Searching resources...",
        "Almost ready..."
    ],
	login:[
	        "Logging in..",
	        "Checking database...",
	        "Are you Human...",
	        "Hacking NASA PC...",
			"Almost ready..."
	    ]

};

let loadingInterval;

function showLoader(type="default"){

    const overlay=document.getElementById("loadingOverlay");
    const message=document.getElementById("loadingMessage");

    overlay.classList.add("show");

    let list=loadingMessages[type] || loadingMessages.default;

    let index=0;

    message.innerText=list[0];

    loadingInterval=setInterval(()=>{

        index=(index+1)%list.length;

        message.innerText=list[index];

    },1700);

}

function hideLoader(){

    clearInterval(loadingInterval);

    document.getElementById("loadingOverlay").classList.remove("show");

}


// ===========================================
// Automatic Loader for Navigation
// ===========================================

let loaderVisible = false;

document.addEventListener("DOMContentLoaded", () => {

    document.querySelectorAll("a").forEach(link => {

        link.addEventListener("click", function () {

            // Already showing
            if(loaderVisible) return;

            const href = this.getAttribute("href");

            // Ignore invalid links
            if(
                !href ||
                href === "#" ||
                href.startsWith("javascript:") ||
                href.startsWith("mailto:") ||
                href.startsWith("tel:")
            ){
                return;
            }

            // Ignore anchors
            if(href.startsWith("#")){
                return;
            }

            // Ignore opening in new tab
            if(this.target === "_blank"){
                return;
            }

            loaderVisible = true;

            // Read custom loader type
            const type = this.dataset.loader || "default";

            showLoader(type);

        });

    });

});