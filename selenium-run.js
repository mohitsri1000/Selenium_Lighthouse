import puppeteer from 'puppeteer';
import readline from 'readline';
import {startFlow, desktopConfig} from 'lighthouse';
import {writeFileSync} from 'fs';

const args = process.argv.slice(2);
const portArg = args.find(arg => arg.startsWith('--port='));
const port = portArg ? portArg.split('=')[1] : '9222';

const urlArg = args.find(arg => arg.startsWith('--url='));
const url = urlArg.split('=')[1];

const reportNameArg = args.find(arg => arg.startsWith('--reportName='));
const reportName = reportNameArg ? reportNameArg.split('=')[1] : 'report';

const timeoutArg = args.find(arg => arg.startsWith('--timeout='));
const timeoutValue = timeoutArg ? parseInt(timeoutArg.split('=')[1]) : 300000;

const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
});

const browser = await puppeteer.connect({
    browserURL: `http://localhost:${port}`,
  });

const page = await browser.newPage();
const flow = await startFlow(page, {
    config: desktopConfig,
  });

// Phase 1 - Navigate to the landing page.
await flow.navigate(url);

// Phase 2 - Interact with the page and submit the search form.
await flow.startTimespan();

//wait for information that all steps are done
await new Promise((resolve, reject) => {
    const timeout = setTimeout(() => {
      reject(new Error('Czas oczekiwania upłynął. Brak odpowiedzi.'));
      rl.close();
    }, timeoutValue);
  
    rl.question('Write FINISH to end Timespan:\n', (answer) => {
      clearTimeout(timeout);
      if (answer === 'FINISH') {
        resolve();
      } else {
        reject(new Error('Wpisano niepoprawne polecenie.'));
      }
      rl.close();
    });
  });

await flow.endTimespan();

// Phase 3 - Analyze the new state.
await flow.snapshot();
// Get the comprehensive flow report.
writeFileSync(`${reportName}.html`, await flow.generateReport());
// Save results as JSON.
writeFileSync(`${reportName}-flow-result.json`, JSON.stringify(await flow.createFlowResult(), null, 2));

// Cleanup. - not needed if the browser is managed from the JAVA level
// await browser.close();

process.exit(0)