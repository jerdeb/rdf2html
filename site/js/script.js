var menulinks = "";
var ser = "";
$(document).ready(function() {
	$.getJSON( "params.json", function( data ) {
		// load footer items
		$("#title").text(data['topbar']['title']);
		$("#menutitle").text(data['topbar']['title']);

		links = data['topbar']['links'];
		$.each(links, function(item) {
			if (links[item] === "home") menulinks = menulinks.concat("<a href='index.html'>home</a> | ");
			else menulinks = menulinks.concat("<li><a href='"+links[item].toLowerCase()+".html'>"+links[item]+"</a></li> ");
		});
		$("#menuitems").html(menulinks);

		serialisation = data['serialisations'];
		$.each(serialisation, function(item) {
			ser = ser.concat("<a class='label label-primary' href='"+negotiate(serialisation[item])+"'>"+serialisation[item]+"</a>&nbsp;");
		})
		$("#serialisation").html(ser);

		//load settings
		if(data['settings']['topbar'] == false){
			$("#topbar").css('visibility', 'hidden');
			$("#menuitems").css('visibility', 'hidden');
		}
	});
});

$(function() {
    $('a').tooltip({placement: 'bottom'});
});


function loadFile(path)
{

	var stringData = $.ajax({
	                    url: path,
	                    async: false
	}).responseText;

	return stringData;
}

function negotiate(serialisation){
	var str = document.URL;
	str = str.replace(".html","");
	if (serialisation == "rdf/xml")
		return str+".rdf";
	if (serialisation == "turtle")
		return str+".ttl";

	//TODO more serialisations
}
