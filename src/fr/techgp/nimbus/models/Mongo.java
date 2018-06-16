package fr.techgp.nimbus.models;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class Mongo {

	// Connection à MongoDB (mongoClient est "thread-safe")
	private static MongoClient mongoClient = null; // = new MongoClient("localhost", 27017)
	// Connection à une base de données (et créaton si elle n'existe pas encore)
	private static MongoDatabase mongoDatabase = null; // = mongoClient.getDatabase("mydb");

	public static final void init(String host, int port, String database) {
		// Connection la première fois
		if (mongoClient == null) {
			synchronized (Mongo.class) {
				mongoClient = new MongoClient(host, port);
				mongoDatabase = mongoClient.getDatabase(database);
			}
		}
	}

	public static final MongoCollection<Document> getCollection(String collectionName) {
		return mongoDatabase.getCollection(collectionName);
	}

	public static final MongoCollection<Document> getWriteCollection(String collectionName) {
		return mongoDatabase.withWriteConcern(WriteConcern.JOURNALED).getCollection(collectionName);
	}

	public static final MongoCollection<Document> getCollection(String collectionName, WriteConcern writeConcern) {
		return mongoDatabase.withWriteConcern(writeConcern).getCollection(collectionName);
	}

}
