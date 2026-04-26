//package logic;
//import java.awt.Color;
//import java.util.Scanner;
//import piece.Knight;
//public class main {
//	public static void main(String[] args) {
//		// TODO Auto-generated method stub
//		
//		Player whitePlayer = new Player(Color.white, true);
//		Player blackPlayer = new Player(Color.black,false);
//		GameLogic game = new GameLogic(whitePlayer,blackPlayer);
//		Scanner scanner = new Scanner(System.in);
//		while(game._gameInProcess) {
//			if(game._state.get_currentTurn() == Color.white)
//				System.out.println("Turn: White");
//			else
//				System.out.println("Turn: Black");
//			System.out.println("enter x,y you would like to move from, and x,y you want to move to");
//			try {
//				int fromX = scanner.nextInt();
//				int fromY = scanner.nextInt();
//				int toX = scanner.nextInt();
//				int toY = scanner.nextInt();
//				
//				if(game.PlayerMove(fromX, fromY, toX, toY)==true) {
//					System.out.println("valid movement");
//				}
//				else
//				{
//					System.out.println("invalid movement");
//				}
//			}
//			catch(NullPointerException e) {
//				System.out.println("cords cannot be null");
//				
//			}
//			catch(ArrayIndexOutOfBoundsException e) {
//				System.out.println("cords are out of array");
//			}
//			catch(IllegalStateException e) {
//				System.out.println("You may only move your own Tools");
//			}
//			catch(Exception e) {
//				System.out.println("invalid input");
//			}
//			if(game._state.is_isCheckmate()==true) {
//				game._gameInProcess=false;
//			}
//			if(game._state.is_isStalemate()==true) {
//				game._gameInProcess=false;
//			}
//		}
//		
//		
//		
//	}
//	
//}
//
//
//
