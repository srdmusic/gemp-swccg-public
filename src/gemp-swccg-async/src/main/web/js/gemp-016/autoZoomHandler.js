class AutoZoom {
	showPreviewImage = true;
	previewImageBPID = 0;
	cookieName = null;
	
	isTouchDevice = false;
	
	autoZoomToggle = null;
	previewImageDiv = null;
	cardDisplay = null;
	
	previewImage = null;
	flipMessageDiv = null;
	//The actual card on the table, referenced so that we know
	// what the original rotation is.
	baseImageDiv = null;

	constructor(cookieName) {
		const that = this;
		this.cookieName = cookieName;
		this.isTouchDevice = 'ontouchstart' in document.documentElement;
		const cookie = $.cookie(this.cookieName);
		
		//An unset cookie should default to true.
		if(cookie == "false") {
			this.showPreviewImage = false;
		}
		else {
			this.showPreviewImage = true;
		}
		
		if(!this.isTouchDevice) {
			this._setupToggleButton();
		}

		this.previewImageDiv = $('<div>', {
			id: 'previewImage',
			class: 'previewImage',
			style: ""
		}).appendTo('body');
		
		this.cardDisplay = new CardDisplay();
		this.cardDisplay.baseDiv.appendTo(this.previewImageDiv);
		this.cardDisplay.baseDiv.css({
			position: "absolute"
		});
		
		this.flipMessageDiv = $('<div>', {
			id: 'auto-zoom-message'
		}).appendTo(this.cardDisplay.baseDiv);
		
		this.previewImageDiv = this.previewImageDiv[0];		
	}
	
	_setupToggleButton() {
		const that = this;
		const enabledIcon = "ui-icon-search";
		const disabledIcon = "ui-icon-circle-close";
		
		const startingIcon = this.showPreviewImage ? enabledIcon : disabledIcon;
		
		this.autoZoomToggle = $("<button id='auto-zoom-toggle'>Auto-zoom cards on hover</button>").button(
		{
			icons:{
				primary:startingIcon
			}, 
			text:false
		});
		
		this.autoZoomToggle.click(
			function () {
				if (that.showPreviewImage) {
					that.autoZoomToggle.button("option", "icons", {primary:disabledIcon});
					that.showPreviewImage = false;
					that.saveCookieValue();
				} else {
					that.autoZoomToggle.button("option", "icons", {primary:enabledIcon});
					that.showPreviewImage = true;
					that.saveCookieValue();
				}
			});
	}
	
	saveCookieValue() {
		$.cookie(this.cookieName, "" + this.showPreviewImage, { expires: 365 });
	}
	
	// make the preview image shown be the reference image that's hovered on:
	displayPreviewImage(refImageDiv, card) {
	
		const that = this;
		
		// this.previewImage.onload = function () {
			
		//that.previewImage.style.display = "block";
		
		// get position and size of the reference image (actually the parent div):
		var rect = refImageDiv.getBoundingClientRect();
		var srcImageX = rect.left;
		var srcImageY = rect.top;
		var srcImageWidth = rect.right - rect.left;
		var srcImageHeight = rect.bottom - rect.top;
		// get the size of the browser window:
		var windowWidth = window.innerWidth;
		var windowHeight = window.innerHeight;
		var windowRatio = windowWidth / windowHeight;
		
		//We want horizontal and vertical representations of cards to 
		// match the same size, else depending on the screen some cards will
		// be big and others small based entirely on their orientation.
		var maxLongSide  = windowHeight * 0.9;
		
		//If we are on a vertically-oriented browser window (not a phone, 
		// because this feature is disabled on mobile)
		if(windowRatio <= 1) {
			maxLongSide = windowWidth * 0.9;
		}
		
		var maxShortSide = maxLongSide * CardDisplay.TargetVertRatio;
		
		// Some cards are remastered at a higher resolution, but not all.  So we
		// will stretch the lower-res cards to the higher resolution, to avoid 
		// disparate sizes, while keeping them within the max window bounds.
		var targetLong = Math.min(maxLongSide, CardDisplay.TargetLong);
		var targetShort = Math.min(maxShortSide, CardDisplay.TargetShort);

		

		if(card.horizontal || card.effectivelyHorizontal()) {
			this.cardDisplay.reloadFromCard(card, targetLong, targetShort);
		}
		else {
			this.cardDisplay.reloadFromCard(card, targetShort, targetLong);
		}

		// var previewImageWidth = this.cardDisplay.baseDiv.width;
		// var previewImageWidth = this.cardDisplay.baseDiv.width;

		// // horizontal cards
		// if(card.horizontal || card.effectivelyHorizontal()) {
		// 	previewImageHeight = targetShort;
		// 	previewImageWidth  = targetLong;
		// }
		// // vertical cards
		// else {
		// 	previewImageHeight = targetLong;
		// 	previewImageWidth  = targetShort;
		// }

		var previewImageWidth = this.cardDisplay.baseDiv.width();
		var previewImageHeight = this.cardDisplay.baseDiv.height();

		var imageRatio = previewImageWidth / previewImageHeight;

		// set the horizontal position of the preview image:
		const rightEdge = srcImageX + srcImageWidth - 15;
		const leftEdge = srcImageX;
		const goesPastRightBound = rightEdge + previewImageWidth > windowWidth;
		const goesPastLeftBound = leftEdge - previewImageWidth < 0;
		var previewImageLeft = rightEdge;
		
		if (goesPastRightBound && goesPastLeftBound) {
			// if previewImage would extend past either left or right side
			// of screen, (i.e. it is the center location on a narrow display)
			// then we must find the best place to put it.
			
			//if(srcImageY > windowHeight / 2)
			//display the previewImage in the biggest space 
			// available and shrink to fit
			const rightSpace = windowWidth - (leftEdge + srcImageWidth);
			const leftSpace = leftEdge;
			
			// const topSpace = windowHeight - (topEdge)
			if (rightSpace > leftSpace) {
				previewImageWidth = rightSpace;
				previewImageLeft = rightEdge;
			}
			else {
				previewImageWidth = leftSpace;
				previewImageLeft = leftEdge - previewImageWidth;
			}
			previewImageHeight = previewImageWidth / imageRatio;
		}
		else {
			if (goesPastRightBound) {
				previewImageLeft = leftEdge - previewImageWidth;
			}
			else if (goesPastLeftBound) {
				previewImageLeft = rightEdge;
			}
		}

		console.log("srcImageY: " + srcImageY);
		console.log("srcImageHeight: " + srcImageHeight);
		console.log("previewImageHeight: " + previewImageHeight);
		// set the vertical position of the preview image (and make sure it isn't extending over the edge of the window):
		var previewImageTop = (srcImageY + (srcImageHeight / 2)) - (previewImageHeight / 2);
		console.log("previewImageTop: " + previewImageTop);
		if ((previewImageTop + previewImageHeight + 15) > windowHeight) {
			previewImageTop = windowHeight - previewImageHeight - 15;
		}
		else if (previewImageTop < 0) {
			previewImageTop = 0;
		}

		console.log("previewImageTop: " + previewImageTop);

		this.cardDisplay.baseDiv[0].style.left = previewImageLeft + "px";
		this.cardDisplay.baseDiv[0].style.top = previewImageTop + "px";
		
		previewImageTop = (srcImageY + (srcImageHeight / 2)) - (previewImageHeight / 2);;
		//previewImageLeft = srcImageX;

		
	}

	hidePreviewImage() {
		this.cardDisplay.clear();

		this.hidePreviewMessage();
	}

	invertPreviewImage(shiftHeld) {
		const baseRotated = this.baseImageDiv.style.transform.includes("180");
		//If the base image is already rotated (such as a location facing 
		// the player), then we act as if Shift is held, even if it's not.  
		// However if shift IS held AND it's rotated, we act like it's not.  
		// This is basically XOR; when they are the same they cancel out,
		// but when they are different they cause a rotation.
		this.cardDisplay.setInvert((shiftHeld != baseRotated));
	}
	
	setPreviewMessage(reversible) {
		let message = "";
		
		if(reversible) {
			message = "Press <b>[Shift]</b> to flip.";
		}
		else if(this.baseImageDiv.style.transform.includes("180")) {
			message = "Hold <b>[Shift]</b> to rotate.";
		}
		
		if(message) {
			this.flipMessageDiv.html(message);
			this.flipMessageDiv[0].style.display = "block";
		}
		else {
			this.hidePreviewMessage();	
		}
	}
	
	hidePreviewMessage() {
		this.flipMessageDiv.html("");
		this.flipMessageDiv[0].style.display = "none";
	}
	
	handleMouseOver(event, isDragging, infoDialogOpen) {
		const target = $(event.target);
		const tarIsCard = target.hasClass("actionArea");
		
		// Reasons to cancel the popup: we're on a touch device,
		// auto zoom has been disabled, we're not hovering over a card,
		// we are currently click-dragging, the card preview box is open.
		if(this.isTouchDevice || !this.showPreviewImage
		   || !tarIsCard || isDragging || infoDialogOpen) {
			
			if (this.cardDisplay.populated) {
				this.hidePreviewImage();
				event.stopPropagation();
				return false;
			}
			
			return true;
		}

		const refCard = target.closest(".card");
		this.baseImageDiv = refCard[0];
		const card = refCard.data("card");
		
		// don't show preview image if card is animating
		if (!$(this.baseImageDiv).hasClass('card-animating')) {

			let bp = card.bareBlueprint;
			// don't show preview image if hovered card is the DS/LS card back art
			if (bp !== "-1_1" && bp !== "-1_2") {
				this.displayPreviewImage(this.baseImageDiv, card);
				this.invertPreviewImage(event.shiftKey);
				this.setPreviewMessage(this.cardDisplay.reversible);
				
				event.stopPropagation();
				return false;
			}
		}
		else if (this.cardDisplay.populated) {
			this.hidePreviewImage();
			event.stopPropagation();
			return false;
		}
		
		return true;
	}
	
	handleMouseDown(event) {
		if (this.cardDisplay.populated) {
			this.hidePreviewImage();
		}
	}
	
	handleKeyDown(event) {
		if (!event.repeat && this.showPreviewImage && !this.isTouchDevice 
				&& event.key === "Shift" && this.cardDisplay.populated) {
			
			if(this.cardDisplay.reversible) {
				this.cardDisplay.invert();
			}
			else {
				this.invertPreviewImage(true);
			}
			
		}
		
		return true;
	}
	
	handleKeyUp(event) {
		if (this.showPreviewImage && !this.isTouchDevice 
				&& event.key === "Shift" && this.cardDisplay.populated) {
			//This makes only presses work for reversibles
			if(!this.cardDisplay.reversible) {
				this.invertPreviewImage(false);
			}
		}
		
		return true;
	}
	
}
