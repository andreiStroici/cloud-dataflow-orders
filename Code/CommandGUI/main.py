import sys
import socket
import json
from PyQt5.QtWidgets import QApplication, QWidget, QVBoxLayout, QFormLayout, QLineEdit, QComboBox, QPushButton, \
    QSpinBox, QLabel


class ComandaApp(QWidget):
    def __init__(self):
        super().__init__()

        self.setWindowTitle('Plasează o comandă')
        self.setGeometry(100, 100, 400, 300)

        # Creăm layout-ul principal
        layout = QVBoxLayout()

        # Adăugăm un formular pentru comenzi
        form_layout = QFormLayout()

        # Lista de produse
        self.lista_produse = [
            "Masca protectie",
            "Vaccin anti-COVID-19",
            "Combinezon",
            "Manusa chirurgicala"
        ]

        # Creăm câmpurile din formular
        self.produs_combo = QComboBox()
        self.produs_combo.addItems(self.lista_produse)
        form_layout.addRow("Alege un produs:", self.produs_combo)

        self.cantitate_spin = QSpinBox()
        self.cantitate_spin.setMinimum(1)
        form_layout.addRow("Cantitate:", self.cantitate_spin)

        self.nume_input = QLineEdit()
        form_layout.addRow("Nume client:", self.nume_input)

        self.adresa_input = QLineEdit()
        form_layout.addRow("Adresă de livrare:", self.adresa_input)

        # Buton pentru trimiterea comenzii
        self.submit_button = QPushButton('Plasează comanda')
        self.submit_button.clicked.connect(self.trimite_comanda)

        # Adăugăm formularul și butonul la layout
        layout.addLayout(form_layout)
        layout.addWidget(self.submit_button)

        self.setLayout(layout)

    def trimite_comanda(self):
        produs_comandat = self.produs_combo.currentText()
        cantitate = self.cantitate_spin.value()
        nume_client = self.nume_input.text()
        adresa_livrare = self.adresa_input.text()

        # Creăm obiectul comanda
        comanda = {
            "identitate_client": nume_client,
            "produs_comandat": produs_comandat,
            "cantitate": cantitate,
            "adresa_livrare": adresa_livrare
        }

        # Convertem comanda în format JSON
        comanda_json = json.dumps(comanda)

        # Detalii pentru conexiune TCP
        server_address = ('localhost' , 12345)  # Serverul Spring Boot la care te conectezi (pe TCP)

        # Creăm un socket TCP
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as client_socket:
            try:
                # Conectăm la server
                client_socket.connect(server_address)

                # Trimitem mesajul JSON la server
                client_socket.sendall(comanda_json.encode('utf-8'))

                # Așteptăm răspuns de la server
                response = client_socket.recv(1024)
                print(f"Răspunsul serverului: {response.decode('utf-8')}")

            except Exception as e:
                print(f"Eroare la trimiterea comenzii: {e}")


if __name__ == '__main__':
    app = QApplication(sys.argv)
    window = ComandaApp()
    window.show()
    sys.exit(app.exec_())
