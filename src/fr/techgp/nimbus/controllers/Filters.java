package fr.techgp.nimbus.controllers;

import java.util.Base64;

import fr.techgp.nimbus.models.User;
import fr.techgp.nimbus.server.Filter;
import fr.techgp.nimbus.server.Request;
import fr.techgp.nimbus.server.Response;
import fr.techgp.nimbus.utils.SparkUtils;
import fr.techgp.nimbus.utils.StringUtils;

public class Filters extends Controller {

	/** Ce filtre s'assure que l'utilisateur est authentifié */
	public static final Filter filterAuthenticatedOrRedirect = (request, response) -> {
		String login = getLogin(request, response, false);
		if (login == null)
			redirect(request, response);
	};

	/** Ce filtre s'assure que l'utilisateur est authentifié en tant qu'administrateur */
	public static final Filter filterAdministratorOrRedirect = (request, response) -> {
		String login = getLogin(request, response, false);
		if (login == null)
			redirect(request, response);
		User user = User.findByLogin(login);
		if (user == null || !user.admin)
			redirect(request, response);
	};

	private static final void redirect(Request request, Response response) {
		String q = request.query();
		if (StringUtils.isNotBlank(q))
			q = "?" + q;
		else
			q = "";
		request.session().attribute("urlToLoad", request.path() + q);
		response.renderRedirect("/login.html");
		SparkUtils.halt();
	}

	/** Ce filtre s'assure que l'utilisateur est authentifié */
	public static final Filter filterAuthenticated = (request, response) -> {
		String login = getLogin(request, response, false);
		if (login == null)
			SparkUtils.haltUnauthorized();
	};

	/** Ce filtre s'assure que l'utilisateur est authentifié en tant qu'administrateur */
	public static final Filter filterAdministrator = (request, response) -> {
		String login = getLogin(request, response, false);
		if (login == null)
			SparkUtils.haltUnauthorized();
		User user = User.findByLogin(login);
		if (user == null || !user.admin)
			SparkUtils.haltForbidden();
	};

	/**
	 * Cette filtre vérifie si l'utilisateur est déjà authentifié.
	 * Sinon, elle permet à l'utilisateur de s'authentifier via une authentification HTTP basique.
	 * Enfin, si les 2 méthodes n'aboutissent pas, elle retourne une erreur HTTP 401 Unauthorized.
	 *
	 *  @see https://en.wikipedia.org/wiki/Basic_access_authentication
	 *  @see Controller#authenticate(String, String)
	 */
	public static final String getLogin(Request request, Response response, boolean allowBasicAuthentication) {
		// L'utilisateur est-il déjà connecté ?
		String login = request.session().attribute("userLogin");
		if (login == null && allowBasicAuthentication) {
			// L'utilisateur n'est pas connecté mais envoie-t-il ses identifiants ?
			String authorization = request.header("Authorization");
			if (authorization != null && authorization.startsWith("Basic")) {
				// On décode
				String base64Credentials = authorization.substring("Basic".length()).trim();
				String textCredentials = new String(Base64.getDecoder().decode(base64Credentials));
				String[] loginAndPassword = textCredentials.split(":", 2);
				// On vérifie login/password
				String error = authenticate(loginAndPassword[0], loginAndPassword[1]);
				// Authentification réussie
				if (error == null)
					login = loginAndPassword[0];
			}
			// Demande d'authentification
			if (login == null)
				response.header("WWW-Authenticate", "Basic realm=\"Authentication required\"");
		}
		return login;
	}

}