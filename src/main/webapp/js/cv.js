updateCareerEntries = function(section) {
	var slider = $('#cv-section-slider-'+section)
	var sliderStart = slider.slider("values", 0); var sliderEnd = slider.slider("values", 1);
	$('#cv-section-begin-'+section).html(sliderStart);
	$('#cv-section-end-'+section).html(sliderEnd);
	$('.'+section+'-entry').each(function() {
		if(parseInt($(this).find('.'+section+'-entry-start').first().html()) <= sliderEnd && parseInt($(this).find('.'+section+'-entry-end').first().html()) >= sliderStart)
			$(this).show()
		else
			$(this).hide()
	})
}
$(document).ready(function() {
	var newHeight = $(window).height() - ($('#site-header').height()+($('#site-footer').height()+10)+$('#cv-page-header').height()+2*parseInt($('body').css('margin-top')))
//	alert(newHeight)
	$('#cv-page-body').height(newHeight)
	$('#cv-page-padding').width($('.cv-section-head-container').first().width())
	var firstSection = true
	$('.cv-section-head').each(function() {
		var body = $('#'+$(this).attr('id').replace('-head-','-body-'))
		if(firstSection) { $(this).addClass('cv-section-head-selected'); body.show(); firstSection = false; }
		else body.hide()
	})
	$('.cv-section-head').click(function() {
		var body = $('#'+$(this).attr('id').replace('-head-','-body-'))
		if($(this).hasClass('cv-section-head-selected')) {
			$(this).removeClass('cv-section-head-selected')
			body.hide()
		} else {
			$(this).addClass('cv-section-head-selected')
			body.show()
		}
	})
	$("#cv-section-slider-career").slider({ range: true, min: 2000, max: 2012, values: [ 2010, 2012 ],
		change: function() { updateCareerEntries('career') }
	})
	$("#cv-section-slider-education").slider({ range: true, min: 2000, max: 2012, values: [ 2000, 2012 ],
		change: function() { updateCareerEntries('education') }
	})
	updateCareerEntries('career')
	updateCareerEntries('education')
})