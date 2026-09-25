const target = document.querySelector('#operations');
try {
  const response = await fetch('/openapi.json');
  if (!response.ok) throw new Error('Specification unavailable.');
  const spec = await response.json();
  target.replaceChildren();
  for (const [path, methods] of Object.entries(spec.paths)) for (const [method, operation] of Object.entries(methods)) {
    const section = document.createElement('section');
    const title = document.createElement('h2'); title.textContent = method.toUpperCase() + ' ' + path;
    const summary = document.createElement('p'); summary.textContent = operation.summary;
    const details = document.createElement('details');
    const label = document.createElement('summary'); label.textContent = 'Parameters, body and responses';
    const pre = document.createElement('pre'); pre.textContent = JSON.stringify(operation, null, 2);
    details.append(label, pre); section.append(title, summary, details); target.append(section);
  }
} catch (error) { target.textContent = error.message; }
