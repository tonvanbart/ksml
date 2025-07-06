import yaml
import os

os.makedirs('docs/_snippets', exist_ok=True)

with open('ksml/src/test/resources/pipelines/test-aggregate-inline.yaml') as f:
    data = yaml.safe_load(f)

# Trim or extract as needed
partial = {"pipelines": data.get("pipelines")}

with open('docs/_snippets/example.yaml', 'w') as out:
    yaml.dump(partial, out, sort_keys=False)
