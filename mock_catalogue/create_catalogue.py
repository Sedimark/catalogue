import json
import random

participants = [
    {
        "@id": "https://sedimark.surrey.ac.uk/ecosystem/EarthScope",
        "@type": ["owl:NamedIndividual", "sedi:Participant"],
        "schema:givenName": {"@language": "en", "@value": "Michael"},
        "schema:familyName": {"@language": "en", "@value": "Chen"},
        "schema:email": {
            "@value": "mailto:m.chen@earthscope.org",
            "@type": "rdfs:Literal",
        },
        "schema:accountId": {"@value": "EarthScope", "@type": "rdfs:Literal"},
        "schema:image": {
            "@id": "https://www.egm.io/wp-content/uploads/2023/05/Sedimark-Logo-definitivo.png"
        },
    },
    {
        "@id": "https://eviden.com",
        "@type": ["owl:NamedIndividual", "sedi:Participant"],
        "schema:givenName": "Maxime",
        "schema:familyName": "Costalonga",
        "schema:alternateName": "Maxime Costalonga (Eviden SA)",
        "schema:email": {
            "@id": "mailto:xamcost@xam.simplelogin.com",
        },
        "schema:memberOf": {"@id": "https://eviden.com"},
        "schema:accountId": "xamcostEviden",
        "schema:image": {
            "@id": "https://media.monsterindia.com/logos/xOP_evidseax/jdlogo.gif"
        },
        "foaf:homepage": {"@id": "https://eviden.com/about-us/"},
    },
    {
        "@id": "https://sedimark.surrey.ac.uk/ecosystem/USGS",
        "@type": ["owl:NamedIndividual", "sedi:Participant"],
        "schema:accountId": {"@value": "USGS", "@type": "rdfs:Literal"},
    },
    {
        "@id": "https://sedimark.surrey.ac.uk/ecosystem/JAXA",
        "@type": ["owl:NamedIndividual", "sedi:Participant"],
        "schema:givenName": {"@language": "en", "@value": "Yuki"},
        "schema:familyName": {"@language": "en", "@value": "Tanaka"},
        "schema:email": {"@value": "mailto:y.tanaka@jaxa.jp", "@type": "rdfs:Literal"},
        "schema:accountId": {"@value": "JAXA", "@type": "rdfs:Literal"},
    },
]


images = [
    "https://picsum.photos/200/300",
    "https://picsum.photos/200",
    "https://picsum.photos/480/640",
    "https://picsum.photos/48",
    "https://picsum.photos/640/480",
    "https://picsum.photos/20/100",
    "https://picsum.photos/100/20",
    "https://picsum.photos/50/200",
    "https://picsum.photos/200/50",
    "invalid_url",
    "invalid.png",
    "",
]


d = {
    "@graph": [
        {
            "@id": "https://sedimark.surrey.ac.uk/ecosystem/CVSSP",
            "dct:description": {
                "@type": "rdfs:Literal",
                "@value": "Centre for Vision, Speech and Signal Processing (CVSSP) is one of the largest UK research groups focusing on multimedia signal processing and machine learning.",
            },
            "schema:givenName": {"@language": "en", "@value": "Tarek"},
            "schema:familyName": {"@language": "en", "@value": "Elsaleh"},
            "sedi:hasSelf-Listing": {
                "@id": "https://sedimark.surrey.ac.uk/ecosystem/ehealth-living-lab"
            },
            "http://xmlns.com/foaf/0.1/homepage": {
                "@id": "https://sedimark.surrey.ac.uk/ecosystem/cvssp-homepage"
            },
            "schema:email": {
                "@value": "mailto:jane-doe@xyz.edu",
                "@type": "rdfs:Literal",
            },
            "schema:accountId": {"@value": "CVSSP", "@type": "rdfs:Literal"},
            "schema:image": {
                "@id": "https://www.egm.io/wp-content/uploads/2023/05/Sedimark-Logo-definitivo.png"
            },
            "@type": ["owl:NamedIndividual", "sedi:Participant"],
        },
        {
            "@id": "https://sedimark.surrey.ac.uk/ecosystem/UCD",
            "dct:description": {
                "@type": "rdfs:Literal",
                "@value": "Centre for Vision, Speech and Signal Processing (CVSSP) is one of the largest UK research groups focusing on multimedia signal processing and machine learning.",
            },
            "schema:givenName": {"@language": "en", "@value": "Tarek"},
            "schema:familyName": {"@language": "en", "@value": "Elsaleh"},
            "hasSelf-Listing": {
                "@id": "https://sedimark.surrey.ac.uk/ecosystem/ehealth-living-lab"
            },
            "http://xmlns.com/foaf/0.1/homepage": {
                "@id": "https://sedimark.surrey.ac.uk/ecosystem/cvssp-homepage"
            },
            "schema:email": {
                "@value": "mailto:jane-doe@xyz.edu",
                "@type": "rdfs:Literal",
            },
            "schema:accountId": {"@value": "CVSSP", "@type": "rdfs:Literal"},
            "schema:image": {
                "@id": "https://www.egm.io/wp-content/uploads/2023/05/Sedimark-Logo-definitivo.png"
            },
            "@type": ["owl:NamedIndividual", "sedi:Participant"],
        },
    ],
    "@context": {
        "sedi": "https://w3id.org/sedimark/ontology#",
        "dct": "http://purl.org/dc/terms/",
        "owl": "http://www.w3.org/2002/07/owl#",
        "rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
        "xml": "http://www.w3.org/XML/1998/namespace",
        "xsd": "http://www.w3.org/2001/XMLSchema#",
        "rdfs": "http://www.w3.org/2000/01/rdf-schema#",
        "dcat": "http://www.w3.org/ns/dcat#",
        "vocab": "https://w3id.org/sedimark/vocab#",
        "schema": "https://schema.org/",
        "foaf": "http://xmlns.com/foaf/0.1/",
    },
}

d["@graph"].extend(participants)


def convert_to_ontology(data):
    base_url = "https://sedimark.surrey.ac.uk"
    asset_id = f"{base_url}/asset/{data['title'].lower().replace(' ', '-')}"
    offering_id = (
        f"{base_url}/ecosystem/{data['title'].lower().replace(' ', '-')}-offering"
    )

    data["provider"] = random.choice(participants)["@id"]
    data["image"] = random.choice(images)

    asset = {
        "@id": asset_id,
        "@type": ["owl:NamedIndividual", "vocab:DataAsset"],
        "dct:identifier": {
            "@value": data["title"].lower().replace(" ", "-"),
            "@type": "rdfs:Literal",
        },
        "dct:creator": {"@id": data["provider"]},
        "dct:publisher": {"@id": data["provider"]},
        "dct:title": {"@value": data["title"], "@type": "rdfs:Literal"},
        "dct:description": {
            "@value": data["short_description"],
            "@type": "rdfs:Literal",
        },
        "dcat:keyword": [
            {"@value": keyword, "@language": "en"} for keyword in data["keyword"]
        ],
        "dct:created": {
            "@value": data["created_at"].split("T")[0],
            "@type": "xsd:date",
        },
    }
    if data["image"] != "":
        asset["schema:image"] = {"@id": data["image"]}

    offering = {
        "@id": offering_id,
        "@type": ["owl:NamedIndividual", "sedi:Offering"],
        "dct:title": {"@value": data["title"], "@type": "rdfs:Literal"},
        "dct:description": {
            "@value": data["short_description"],
            "@type": "rdfs:Literal",
        },
        "dct:creator": {"@id": data["provider"]},
        "dct:publisher": {"@id": data["provider"]},
        "sedi:hasAsset": {"@id": asset_id},
        "dct:created": {
            "@value": data["created_at"].split("T")[0],
            "@type": "xsd:date",
        },
    }

    return [asset, offering]


dd = json.loads(open("offerings.json", "r").read())

for g in dd["results"]:
    d["@graph"].extend(convert_to_ontology(g))


with open("catalogue.jsonld", "w") as handle:
    handle.write(json.dumps(d))
