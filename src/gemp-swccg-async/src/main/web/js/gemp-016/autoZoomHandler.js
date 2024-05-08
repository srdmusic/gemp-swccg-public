class AutoZoom {
	showPreviewImage = true;
	previewImageBPID = 0;
	cookieName = null;
	
	isTouchDevice = false;
	
	autoZoomToggle = null;
	previewImageDiv = null;
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
		//this.previewImageDiv.append("<img></img>")
		
		this.previewImage = $('<img></img>')
			.appendTo(this.previewImageDiv)[0];
		
		this.flipMessageDiv = $('<div>', {
			id: 'auto-zoom-message'
		}).appendTo(this.previewImageDiv);
		
		
		//this.previewImage = this.previewImageDiv.find("img")[0];
		
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
	displayPreviewImage(refImageDiv) {
	
		const that = this;
		
		this.previewImage.onload = function () {
			
			that.previewImage.style.display = "block";
			
			// get position and size of the reference image (actually the parent div):
			var rect = refImageDiv.getBoundingClientRect();
			var srcImageX = rect.left;
			var srcImageY = rect.top;
			var srcImageWidth = rect.right - rect.left;
			var srcImageHeight = rect.bottom - rect.top;
			// get the size of the browser window:
			var windowWidth = window.innerWidth;
			var windowHeight = window.innerHeight;
			// get the elements to be altered:
			var previewImageStyle = that.previewImageDiv.style;
			var previewImageImgStyle = that.previewImage.style;
			var previewImageHeight = that.previewImage.naturalHeight;
			var previewImageWidth = that.previewImage.naturalWidth;
			
			var ratio = previewImageWidth / previewImageHeight;

			if (previewImageHeight > windowHeight / 2) {
				previewImageHeight = windowHeight / 2;
				previewImageWidth = ratio * previewImageHeight;
			}
			else if (previewImageWidth > windowWidth / 2) {
				previewImageWidth = windowWidth / 2;
				previewImageHeight = previewImageWidth / ratio;
			}

			// set the horizontal position of the preview image:
			const rightEdge = srcImageX + srcImageWidth;
			const leftEdge = srcImageX;
			const goesPastRightBound = rightEdge + previewImageWidth > windowWidth;
			const goesPastLeftBound = leftEdge - previewImageWidth < 0;
			var previewImageLeft = rightEdge;
			
			if (goesPastRightBound && goesPastLeftBound) {
				// if previewImage would extend past either left or right side
				// of screen, display the previewImage in the biggest space 
				// available and shrink to fit
				const rightSpace = windowWidth - (leftEdge + srcImageWidth);
				const leftSpace = leftEdge;
				if (rightSpace > leftSpace) {
					previewImageWidth = rightSpace;
					previewImageLeft = rightEdge;
				}
				else {
					previewImageWidth = leftSpace;
					previewImageLeft = leftEdge - previewImageWidth;
				}
				previewImageHeight = previewImageWidth / ratio;
			}
			else {
				if (goesPastRightBound) {
					previewImageLeft = leftEdge - previewImageWidth;
				}
				else if (goesPastLeftBound) {
					previewImageLeft = rightEdge;
				}
			}

			// set the vertical position of the preview image (and make sure it isn't extending over the edge of the window):
			var previewImageTop = (srcImageY + (srcImageHeight / 2)) - (previewImageHeight / 2);
			if ((previewImageTop + previewImageHeight + 15) > windowHeight) {
				previewImageTop = windowHeight - previewImageHeight - 15;
			}
			else if (previewImageTop < 0) {
				previewImageTop = 0;
			}

			// assign the positions to the preview image element:
			previewImageStyle.left = previewImageLeft + "px";
			previewImageStyle.top = previewImageTop + "px";
			previewImageImgStyle.width = previewImageWidth + 'px';
			previewImageImgStyle.height = previewImageHeight + 'px';
		}
		
		let cardImage = Card.getImageUrl(this.previewImageBPID);

		if (cardImage != null) {
			this.previewImage.src = cardImage;
		}
	}

	hidePreviewImage() {
		this.previewImageBPID = "0";
		this.previewImage.src = "";
		this.previewImage.style.display = "none";
		this.previewImage.style.transform = "rotate(0deg)";
		this.hidePreviewMessage();
	}

	rotatePreviewImage(shiftHeld, reversible) {
		const baseRotated = this.baseImageDiv.style.transform.includes("180");
		//If the base image is already rotated (such as a location facing 
		// the player), then we act as if Shift is held, even if it's not.  
		// However if shift IS held AND it's rotated, we act like it's not.  
		// This is basically XOR; when they are the same they cancel out,
		// but when they are different they cause a rotation.
		// Also, if this card is a reversible (like an objective), just 
		// always make it face right-side up.
		if ((shiftHeld == baseRotated) || reversible) {
			this.previewImage.style.transform = "rotate(0deg)";
		}
		else {
			this.previewImage.style.transform = "rotate(180deg)";
		}
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
	
	getShiftedId(blueprintId, isHoldingShift) {
		const reverseId = this.getReverseId(blueprintId);

		return isHoldingShift && reverseId
				? reverseId : blueprintId;
	}
	
	getReverseId(blueprintId) {
		if(blueprintId.includes("_BACK"))
			return blueprintId.replace("_BACK", "");
		
		if(Card.getImageUrl(blueprintId + "_BACK"))
			return blueprintId + "_BACK";
		
		return null;
	}
	
	isReversible(blueprintId) {
		return blueprintId.includes("_BACK") ||
			Card.getImageUrl(blueprintId + "_BACK");
	}

	handleMouseOver(event, isDragging, infoDialogOpen) {
		const target = $(event.target);
		const tarIsCard = target.hasClass("actionArea");
		
		// Reasons to cancel the popup: we're on a touch device,
		// auto zoom has been disabled, we're not hovering over a card,
		// we are currently click-dragging, the card preview box is open.
		if(this.isTouchDevice || !this.showPreviewImage
		   || !tarIsCard || isDragging || infoDialogOpen) {
			
			if (this.previewImageBPID !== "0") {
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
			const imageBlueprintId = this.getShiftedId(card.bareBlueprint, event.shiftKey);

			// don't show preview image if hovered card is the DS/LS card back art
			if (imageBlueprintId !== "-1_1" && imageBlueprintId !== "-1_2") {
				this.previewImageBPID = imageBlueprintId;
				this.displayPreviewImage(this.baseImageDiv);
				this.rotatePreviewImage(event.shiftKey, imageBlueprintId !== card.bareBlueprint);
				this.setPreviewMessage(this.isReversible(card.bareBlueprint));
				
				event.stopPropagation();
				return false;
			}
		}
		else if (this.previewImageBPID !== "0") {
			this.hidePreviewImage();
			event.stopPropagation();
			return false;
		}
		
		return true;
	}
	
	handleMouseDown(event) {
		if (this.previewImageBPID !== 0) {
			this.hidePreviewImage();
		}
	}
	
	handleKeyDown(event) {
		if (!event.repeat && this.showPreviewImage && !this.isTouchDevice 
				&& event.key === "Shift" && this.previewImageBPID != "0") {
			
			const imageBlueprintId = this.getShiftedId(this.previewImageBPID, true);
			const reversible = imageBlueprintId !== this.previewImageBPID;
			const image = Card.getImageUrl(imageBlueprintId);
			
			this.previewImageBPID = imageBlueprintId;
			this.previewImage.src = image;
			this.rotatePreviewImage(true, reversible);
		}
		
		return true;
	}
	
	handleKeyUp(event) {
		if (this.showPreviewImage && !this.isTouchDevice 
				&& event.key === "Shift" && this.previewImageBPID != "0") {
			
			const imageBlueprintId = this.getShiftedId(this.previewImageBPID, false);
			const reversible = imageBlueprintId !== this.previewImageBPID;
			const image = Card.getImageUrl(imageBlueprintId);
			
			this.previewImageBPID = imageBlueprintId;
			this.previewImage.src = image;
			this.rotatePreviewImage(false, reversible);
		}
		
		return true;
	}
	
}