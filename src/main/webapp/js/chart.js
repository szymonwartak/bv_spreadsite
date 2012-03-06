

var interval = 1; // interval between prices
var maxDate = 0, minDate = 99999999999;
var maxPrice = 0, minPrice = 99999999999;
var maxQuantity = 0, minQuantity = 99999999999;
var canvasHeight = 400, canvasWidth = 800, canvasBufferY = 10, canvasBufferX = 10, rightBuffer = 30;
var barHeight = 0, barWidth = 2;
var xAxisWidth = 50, xAxisHeight = 20, yAxisWidth = 50, yAxisHeight = 20;
var totalHeight, heightOffset, totalWidth, widthOffset;
var textHeight = 10, maxXLabelWidth = 40, maxYLabelWidth = 40;
var previousClosePrice = -1, previousClosePriceX = 0;
var xdraw = 0;
var priceColour = "#0f0";
var barHoverData = {}, BARS_PER_TIME = 16;

function resetVariables() {
	barHoverData = {};
	maxDate = new Date(0), minDate = new Date();
	maxPrice = 0, minPrice = 99999999999;
	maxQuantity = 0, minQuantity = 99999999999;
	previousClosePrice = -1, previousClosePriceX = 0;
}

function updateData(newData, chartId) {
	var orderBlocks = jQuery.parseJSON(newData);
	updateChart(chartId, orderBlocks);
}
function showBarData(x,y) {
	var mod = 1, xOffset = x;
	$("#barPrice").html(0);
	$("#barQuantity").html(0);
	for(var i1=0; i1<barWidth; i1++) {
		var xOffset = xOffset+(mod*i1);
		mod = mod*(-1);
		if(barHoverData[xOffset] != null) {
			for(var i2=0; i2<BARS_PER_TIME; i2++) {
				if(barHoverData[xOffset][i2] != null && barHoverData[xOffset][i2].y > (y-barHeight/2) && barHoverData[xOffset][i2].y < (y+barHeight/2)) {
					$("#barPrice").html(barHoverData[xOffset][i2].price);
					$("#barQuantity").html(barHoverData[xOffset][i2].quantity);
					//alert(barHoverData[xOffset][i2].price+" "+barHoverData[xOffset][i2].date);
					break;
				}
			}
			break;
		}
	}
}

function updateChart(chartId, orderBlocks) {
	resetVariables();
	var graphCanvas = $("#" + chartId);
	$("#" + chartId).mousemove(function(e) {
		//showBarData($(this).offset().left, $(this).offset().top);
		if($.browser.mozilla){
			showBarData(Math.round(e.clientX-graphCanvas.offset().left), Math.round(e.clientY-graphCanvas.offset().top));
		} else {
			showBarData(e.offsetX, e.offsetY);
		}
	});
	// Ensure that the element is available within the DOM
	if (graphCanvas && graphCanvas[0].getContext) {
		// determine the mins and maxes to scale the graph and colours
		for ( var i1 = 0; i1 < orderBlocks.length; i1++) {
			orderBlocks[i1].orderBlock.blockDate = orderBlocks[i1].orderBlock.blockDate;
			if (orderBlocks[i1].orderBlock.price + 1 > maxPrice)
				maxPrice = orderBlocks[i1].orderBlock.price + 1;
			if (orderBlocks[i1].orderBlock.price - 1 < minPrice)
				minPrice = orderBlocks[i1].orderBlock.price - 1;
			if (orderBlocks[i1].orderBlock.closePrice > maxPrice)
				maxPrice = orderBlocks[i1].orderBlock.closePrice;
			if (orderBlocks[i1].orderBlock.closePrice < minPrice)
				minPrice = orderBlocks[i1].orderBlock.closePrice;
			if (orderBlocks[i1].orderBlock.quantity > maxQuantity)
				maxQuantity = orderBlocks[i1].orderBlock.quantity;
			if (orderBlocks[i1].orderBlock.quantity < minQuantity)
				minQuantity = orderBlocks[i1].orderBlock.quantity;
			if (orderBlocks[i1].orderBlock.blockDate > maxDate)
				maxDate = orderBlocks[i1].orderBlock.blockDate;
			if (orderBlocks[i1].orderBlock.blockDate < minDate)
				minDate = orderBlocks[i1].orderBlock.blockDate;
		}

		canvasHeight = graphCanvas.attr("height");
		canvasWidth = graphCanvas.attr("width");
		canvasBufferY = 10;// canvasHeight*0.1/2; // 5% buffer around the edges
		canvasBufferX = 10;// canvasWidth*0.1/2; // 5% buffer around the edges
		for ( var i1 = 0; i1 < 2; i1++) { // after a few iterations, these dependant values should converge
			canvasDrawableHeight = canvasHeight - 2 * canvasBufferY - xAxisHeight;
			canvasDrawableWidth = canvasWidth - 2 * canvasBufferX - yAxisWidth - rightBuffer;
			barHeight = y(0) - y(1);
		}
		// Open a 2D context within the canvas
		var context = graphCanvas[0].getContext('2d');
		context.clearRect(0,0,canvasWidth, canvasHeight);
		context.font = textHeight + "px Arial";

		// draw background
		context.fillStyle = "rgb(255,255,0)";
		drawRectangle(context, 0, 0, canvasWidth, canvasHeight);

		// draw axes
		drawXAxis(context);
		drawYAxis(context);

		var closePriceMap = new OrderedMap();
		// draw the market depth bars
		for ( var i1 = 0; i1 < orderBlocks.length; i1++) {
			var xcoord = x(orderBlocks[i1].orderBlock.blockDate); 
			drawBar(context, xcoord, orderBlocks[i1].orderBlock.price,
					orderBlocks[i1].orderBlock.quantity,
					orderBlocks[i1].orderBlock.blockDate, 
					orderBlocks[i1].orderBlock.sellOrBuy);
			// this is unnecessary if order blocks are ordered by date
			closePriceMap.put(xcoord, orderBlocks[i1].orderBlock.closePrice); 
		}
		// draw price from feed
		var okeys = closePriceMap.getOrderedKeys();
		for ( var i1 = 0; i1 < okeys.length; i1++)
			drawPriceLine(context, okeys[i1], closePriceMap.get(okeys[i1]));
	}
}
// draw x-axis
function drawXAxis(context) {
	context.fillStyle = "rgb(0,0,0)";
	var xstep = Math.max(1, Math.floor((maxDate - minDate) / 10));
	var tick = minDate;
	while (tick <= maxDate) {
		var xtick = x(tick)
		drawLine(context, xtick, canvasDrawableHeight + canvasBufferY, xtick,
				canvasDrawableHeight + canvasBufferY + 5, "rgb(0,0,0)");
		context.fillText(formatXAxisDate(tick, minDate, maxDate), xtick
				- maxXLabelWidth / 2, canvasHeight - textHeight, maxXLabelWidth);
		tick += xstep;
	}
}
// format x-axis date, based on the min and max dates of the data set to figure
// out how precise we want to be
function formatXAxisDate(date, min, max) {
	var date = new Date(date);
	if (max - min < 1000 * 60 * 60 * 24)
		return date.toLocaleTimeString().substring(0,5);
	else
		return date.getDate() + "/" + (date.getMonth() + 1) + "/" + date.getYear();
}
// draw y-axis
function drawYAxis(context) {
	context.fillStyle = "rgb(0,0,0)";
	var ystep = Math.max(1, Math.floor((maxPrice - minPrice) / 20));
	var tick = Math.ceil(minPrice);
	while (tick <= maxPrice) {
		var ytick = y(tick)
		drawLine(context, x(minDate) - barWidth / 2 - 5, ytick, x(minDate) - barWidth / 2, ytick, "rgb(0,0,0)");
		context.fillText(tick.toString(), 0, ytick + textHeight / 2, maxYLabelWidth);
		tick += ystep;
	}
}
// we're drawing at the xdraw point on which the point lies and the bar stradles
function drawPriceLine(context, xcoord, closePrice, highPrice, lowPrice) {
	if (previousClosePrice != -1)
		drawLine(context, previousClosePriceX, y(previousClosePrice), xcoord, y(closePrice), priceColour);
	//drawCircle(context, xcoord, y(closePrice), 5, priceColour);
	previousClosePriceX = xcoord;
	previousClosePrice = closePrice;
}
function drawBar(context, xcoord, price, quantity, date, sb) {
	// drawRectangle(context,startX + (i * barWidth) + i,(chartHeight -
	// height),barWidth,height,true);
	var color = 225 - Math.floor(225 * (quantity - minQuantity) / (maxQuantity - minQuantity)); // between 0 and 1
	context.fillStyle = "rgb(" + color + "," + color + "," + color + ")";
	var top = y(price + (sb == "S" ? 0 : +1)); // selling up to a price and
												// buying down to a price -
												// create the gap here
	var left = xcoord - barWidth / 2;
	context.fillRect(left, top, barWidth, barHeight);
	// x needs to be floored so it's easily accessible later
	addBarHoverData(Math.floor(left+barWidth/2), top+barHeight/2, quantity, price, date); 
}
// record the bar data for displaying when user hovers over
function addBarHoverData(x, y, quantity, price, date) {
	if(barHoverData[x] == null) {
		barHoverData[x] = new Array();
	}
	barHoverData[x][barHoverData[x].length] = { y:y, quantity:quantity, price:price, date:date };
}
// conversion of price to y coordinate (given precalculated min/max, etc)
function y(p) {
	return canvasHeight - ((p - minPrice) / (maxPrice - minPrice) * canvasDrawableHeight + canvasBufferY + xAxisHeight);
}
// conversion of date to x coordinate (given precalculated min/max, etc)
function x(d) {
	return ((d - minDate) / (maxDate - minDate) * canvasDrawableWidth) + canvasBufferX + yAxisWidth;
}
// drawLine - draws a line on a canvas context from the start point to the end point
function drawLine(context, startx, starty, endx, endy, colour) {
	context.strokeStyle = colour;
	context.beginPath();
	context.moveTo(startx, starty);
	context.lineTo(endx, endy);
	context.closePath();
	context.stroke();
}
// drawRectangle - draws a rectangle on a canvas context using the dimensions specified
function drawRectangle(context, x, y, w, h, fill) {
	context.beginPath();
	context.rect(x, y, w, h);
	context.closePath();
	context.stroke();
	if (fill)
		context.fill();
}
// drawCircle
function drawCircle(context, centreX, centreY, radius, colour) {
	context.fillStyle = colour;
	context.beginPath();
	context.arc(centreX, centreY, radius, 0, Math.PI * 2, true);
	context.closePath();
	context.fill();
}
