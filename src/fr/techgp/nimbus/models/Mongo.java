package fr.techgp.nimbus.models;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

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
				initSequence("items");
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

	public static final void reset(boolean users) {
		if (users)
			getCollection("users").drop();
		getCollection("counters").drop();
		getCollection("items").drop();
		initSequence("items");
	}

	public static final void initSequence(String sequence) {
		MongoCollection<Document> collection = getCollection("counters", WriteConcern.JOURNALED);
		Bson filter = Filters.eq("_id", sequence);
		Document d = collection.find().filter(filter).first();
		if (d == null)
			collection.insertOne(new Document().append("_id", sequence).append("value", 0L));
	}

	public static final Long getNextSequence(String sequence) {
		MongoCollection<Document> collection = getCollection("counters", WriteConcern.JOURNALED);
		Bson filter = Filters.eq("_id", sequence);
		Document operation = new Document().append("$inc", new Document().append("value", 1));
		Document result = collection.findOneAndUpdate(filter, operation);
		return result.getLong("value");
	}

}
