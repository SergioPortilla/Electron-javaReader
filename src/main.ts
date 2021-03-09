import { BrowserWindow } from 'electron';

export default class Main {
    static mainWindow: Electron.BrowserWindow;
    static application: Electron.App;
    static BWindow: any;

    private static onWindowAllClosed() {
        if (process.platform !== 'darwin') {
            Main.application.quit();
        }
    }

    private static onReady() {
        Main.mainWindow = new Main.BWindow({ width: 1200, height: 800, webPreferences: { nodeIntegration: true }, darkTheme: true });
        Main.mainWindow.loadFile('./index.html').then(r => console.log(r));
        Main.mainWindow.on('closed', Main.onWindowAllClosed);
    }

    static main(app: Electron.App, browserWindow: typeof BrowserWindow) {
        Main.BWindow = browserWindow;
        Main.application = app;
        Main.application.on('window-all-closed', Main.onWindowAllClosed);
        Main.application.on('ready', Main.onReady);
    }
}

/** ************************************************************************************* */
export function algo(input: any) {
    let inFile: HTMLElement | null  = document.getElementById("inputfile");
    let outText: HTMLElement | null = document.getElementById("output");
    if (inFile && outText) {
    //     inFile.style.display = 'none';
        outText.style.display = 'block';
    }

    const RGX = {
        enter: /\r?\n/gm,
        import: /((?:import ))(\S*)/,
        class: /(class |(?:public|private|protected)+ class )(\S*)/,
        enum: /(enum |(?:public|private|protected)+ enum )(\S*)/,
        interface: /(interface |(?:public|private|protected)+ interface )(\S*)/,
        interface2: /((?:implements ))(\S*)/,
        reserved: /(if|else|for|try|cath|return|new)/,
        method: /((?:public|private|protected) \w*)(\S*)(;)/
    };
    let file = input.files[0];
    let reader = new FileReader();
    reader.readAsText(file);
    let alguito = document.getElementById('output');
    reader.onload = function() {
        if (alguito && typeof reader.result === "string") {
            let array = reader.result.split(RGX.enter);
            let result: string = "";
            let number = 1;
            array.forEach(line => {
                line = regexResolver(line, RGX.method, 'rgb(148,69,8)','method');
                line = regexResolver(line, RGX.import, 'green', 'import');
                line = regexResolver(line, RGX.class, 'rgb(94,214,144)','class');
                line = regexResolver(line, RGX.enum, 'rgb(15,160,160)','enum');
                line = regexResolver(line, RGX.interface, 'rgb(143,148,8)','interface');
                line = regexResolver(line, RGX.interface2, 'rgb(143,148,8)','interface');
                // line = regexResolver(line, RGX.reserved, 'rgb(109,110,233)');
                result = result + '\n' + '<a class="number">'+number++ + '</a>' + '<a class="line">'+line+'</a>';
            });
            alguito.innerHTML = result;
        }
    };

    reader.onerror = function() {
        console.log(reader.error);
    };
}

const eta = false;

function regexResolver(line: string, regex: RegExp, color: string, element: string = ''): string {
    let data = element === '' ? undefined : document.getElementById(element);
    if (regex.test(line) && line.match(regex)) {
        line = line.replace(regex ,`<div style="color: ${color}">$1</div>$2`);

        if (data) {
            // @ts-ignore
            data.innerHTML += '\n' + line.match(regex)[2];
            data.style.display = 'block';
        }
    }
    return line;
}
