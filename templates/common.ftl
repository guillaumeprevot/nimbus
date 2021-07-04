<#macro head title translated>
	<#if translated>
	<title data-translate="text">${title}</title>
	<#else>
	<title>${title}</title>
	</#if>
	<meta charset="UTF-8">
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<meta http-equiv="X-UA-Compatible" content="IE=edge" />
	<meta name="robots" content="noindex, nofollow" />
	<meta name="viewport" content="width=device-width, initial-scale=1" />
	<link type="image/png" rel="icon" href="/favicon.png" />
</#macro>

<#macro styles>
	<style>
@font-face {
	font-family: 'Material Icons';
	font-style: normal;
	font-weight: 400;
	src: url(/libs/material-icons/material-icons.woff2) format('woff2'),
		 url(/libs/material-icons/material-icons.woff) format('woff'),
		 url(/libs/material-icons/material-icons.ttf) format('truetype');
}
	</style>
	<link type="text/css" rel="stylesheet" href="${stylesheet}" />
	<link type="text/css" rel="stylesheet" href="/libs/material-icons/material-icons.css" />
	<link type="text/css" rel="stylesheet" href="/nimbus.css" />
</#macro>

<#macro scripts>
	<script type="text/javascript" src="/libs/jquery/jquery.min.js"></script>
	<script type="text/javascript" src="/libs/popper/popper.min.js"></script>
	<script type="text/javascript" src="/libs/bootstrap/bootstrap.min.js"></script>
	<script type="text/javascript" src="/nimbus.js"></script>
	<script type="text/javascript" src="/langs/${lang}.js"></script>
</#macro>
